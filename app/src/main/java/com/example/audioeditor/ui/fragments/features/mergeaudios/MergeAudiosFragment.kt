package com.example.audioeditor.ui.fragments.features.mergeaudios

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.adapters.AudioItemAdapter
import com.example.audioeditor.databinding.BottomSheetFadeinBinding
import com.example.audioeditor.databinding.BottomSheetFadeoutBinding
import com.example.audioeditor.databinding.BottomSheetTrimBinding
import com.example.audioeditor.databinding.FragmentMergeAudiosBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.lib.darioscrollruler.ScrollRulerListener
import com.example.audioeditor.lib.rangeview.RangeView
import com.example.audioeditor.models.AudioItemModel
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.getAudioFileDuration
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getMetadataFromFile
import com.example.audioeditor.utils.getOutputFile
import com.example.audioeditor.utils.getTemporaryFileInPrivateDirectory
import com.example.audioeditor.utils.getUriFromPath
import com.example.audioeditor.utils.moveFileFromPrivateToPublicDirectory
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.replaceSpaceWithUnderscore
import com.example.audioeditor.utils.setOnOneClickListener
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections


class MergeAudiosFragment : Fragment(), CommandExecutionCallback {

    private val binding by lazy {
        FragmentMergeAudiosBinding.inflate(layoutInflater)
    }

    private val trimBinding by lazy {
        BottomSheetTrimBinding.inflate(layoutInflater)
    }

    private val fadeInBinding by lazy {
        BottomSheetFadeinBinding.inflate(layoutInflater)
    }

    private val fadeOutBinding by lazy {
        BottomSheetFadeoutBinding.inflate(layoutInflater)
    }

    private var audiosList: MutableList<AudioItemModel> = ArrayList()
    private var inputPathsList = mutableListOf<String>()

    private val selectedUris = mutableListOf<Uri>()

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var outputFile: File? = null

    private val renameDialogBinding by lazy {
        RenameDialogBinding.inflate(layoutInflater)
    }
    private var renameAlertDialog: AlertDialog? = null
    private var renameDialogView: ConstraintLayout? = null


    private var savingAlertDialog: AlertDialog? = null
    private val savingDialogBinding by lazy {
        SavingDialogBinding.inflate(layoutInflater)
    }
    private var savingDialogView: ConstraintLayout? = null


    private var quitAlertDialog: AlertDialog? = null
    private val quitDialogBinding by lazy {
        QuitDialogBinding.inflate(layoutInflater)
    }
    private var quitDialogView: ConstraintLayout? = null

    private var trimBottomSheetShowing = false

    private var extension = "mp3"

    private var cropLeft: Float = 0F
    private var cropRight: Float = 1F

    private var lastFunctionCalled: String? = null

    private var pathsList = mutableListOf<String>()

    private var outputPathTrim = ""
    private var outputPathFadeIn = ""
    private var outputPathFadeOut = ""

    private var merge = false
    private var trim = false
    private var fadein = false
    private var fadeout = false


    private var selectedFadeInTime = "0.0"
    private var selectedFadeOutTime = "0.0"

    private var currentFadeInTime = "0.0"
    private var currentFadeOutTime = "0.0"

    var startingFadeInTime = 0f
    var startingFadeOutTime = 0f

    private var audioUri: Uri? = null

    private val valueUpdateHandler = Handler()
    private var valueUpdateRunnable: Runnable? = null

    private var currentIndex = -1



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        with(binding){

            btnRedo.isClickable=false
            btnUndo.isClickable=false
            btnUndo.isFocusable = false
            btnRedo.isFocusable = false

            btnUpload.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(intent, 100)
            }


            btnPlayPause.setOnOneClickListener {
                context?.performHapticFeedback()
                if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    btnPlayPause.setImageResource(R.drawable.pause_button)
                    updateSeekBar()

                } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    pauseMediaPlayer()
                }
            }


            waveform.onProgressChanged = object : SeekBarOnProgressChanged {
                override fun onProgressChanged(
                    waveformSeekBar: WaveformSeekBar,
                    progress: Float,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        val audioDurationMillis = mediaPlayer.duration
                        val selectedPositionMillis = (progress * audioDurationMillis) / 100
                        tvCurrentDurationWindow.text = selectedPositionMillis.toInt().formatDuration()
                        tvCurrentDuration.text = selectedPositionMillis.toInt().formatDuration()
                        mediaPlayer.seekTo(selectedPositionMillis.toInt())
                    }
                }
            }

            ivMinusLeft.setOnClickListener {
                if (::mediaPlayer.isInitialized) {

                    var currentDuration = mediaPlayer.currentPosition.toFloat() / 1000f
                    val totalDuration  = mediaPlayer.duration.toFloat() / 1000f
                    if(currentDuration >= 1){
                        currentDuration-=1
                        tvCurrentDurationWindow.text = (currentDuration*1000).toInt().formatDuration()
                        tvCurrentDuration.text = (currentDuration*1000).toInt().formatDuration()
                        waveform.progress = ((currentDuration/totalDuration) * 100f)
                        mediaPlayer.seekTo((currentDuration*1000).toInt())
                    }

                }
            }


            ivAddLeft.setOnClickListener {
                if (::mediaPlayer.isInitialized) {

                    var currentDuration = mediaPlayer.currentPosition.toFloat() / 1000f
                    val totalDuration  = mediaPlayer.duration.toFloat() / 1000f
                    if(currentDuration <= totalDuration-1){
                        currentDuration+=1
                        tvCurrentDurationWindow.text = (currentDuration*1000).toInt().formatDuration()
                        tvCurrentDuration.text = (currentDuration*1000).toInt().formatDuration()
                        waveform.progress = ((currentDuration/totalDuration) * 100f)
                        mediaPlayer.seekTo((currentDuration*1000).toInt())
                    }

                }
            }

            viewTrim.setOnOneClickListener {
                if (::mediaPlayer.isInitialized && binding.waveform.isVisible) {
                    pauseMediaPlayer()
                    openTrimBottomSheet()
                }
            }

            viewFadeIn.setOnOneClickListener {
                if (::mediaPlayer.isInitialized && binding.waveform.isVisible) {
                    pauseMediaPlayer()
                    openFadeInBottomSheet()
                }
            }

            viewFadeOut.setOnOneClickListener {
                if (::mediaPlayer.isInitialized && binding.waveform.isVisible) {
                    pauseMediaPlayer()
                    openFadeOutBottomSheet()
                }
            }

            btnUndo.setOnOneClickListener{
                Log.d(TAG, "onViewCreated: undo ${btnUndo.isClickable}")
                Log.d(TAG, "onViewCreated: undo ${currentIndex}")

                if(currentIndex>0 && pathsList.size>0){
                    Log.d(TAG, "onViewCreated: undo if")

                    pauseMediaPlayer()
                    audioUri = pathsList[currentIndex-1].getUriFromPath()
                    audioUri?.let { createMediaPlayer(it) }
                    currentIndex -= 1

                    if(currentIndex<1){
                        disableUndo()
                    }
                    if(currentIndex<pathsList.size-1){
                        enableRedo()
                    }
                }
            }

            btnRedo.setOnOneClickListener{
                Log.d(TAG, "onViewCreated: redo ${btnRedo.isClickable}")
                Log.d(TAG, "onViewCreated: redo ${currentIndex}")

                if(currentIndex<pathsList.size-1){
                    Log.d(TAG, "onViewCreated: redo if")

                    pauseMediaPlayer()
                    audioUri = pathsList[currentIndex+1].getUriFromPath()
                    audioUri?.let { createMediaPlayer(it) }
                    currentIndex += 1
                    if(currentIndex>0){
                        enableUndo()
                    }
                    if(currentIndex==pathsList.size-1){
                        disableRedo()
                    }
                }
            }

            btnSave.setOnOneClickListener {
                if(::mediaPlayer.isInitialized){
                    showRenameDialog()
                }
                else{
                    context?.showSmallLengthToast("Can't Save without a file")
                }

            }

            btnBack.setOnOneClickListener {
                context?.performHapticFeedback()
                showQuitDialog()
            }

        }

    }


    //***************************************** Upload Audios  ***********************************************

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->

                // Check if multiple files were selected
                if (intent.clipData != null) {
                    val clipData = intent.clipData
//                    for (i in 0 until clipData?.itemCount!!) {
                    Log.d("adsfsdf", "onActivityResult: ${clipData?.itemCount!!}")
                    val itemCount = clipData.itemCount.takeIf { it <= 3 } ?: 3
                    if(clipData.itemCount > 3){
                        context?.showSmallLengthToast("3 audios can be merged at a time")
                    }

                    if (audiosList.size<3){
                        for (i in 0 until itemCount) {
                            val uri = clipData.getItemAt(i)?.uri
                            if (uri != null) {
                                selectedUris.add(uri)
                            }
                            val newItem = AudioItemModel(
                                "${
                                    uri?.let {
                                        context?.getFileNameFromUri(
                                            it
                                        )
                                    }
                                }"
                            )
                            audiosList.add(newItem)
                        }
                    }
                    else{
                        context?.showSmallLengthToast("You have already uploaded 3 audios")
                    }
                } else if (intent.data != null) {
                    // Only one file was selected
                    if (audiosList.size<3){
                        val uri = intent.data!!
                        selectedUris.add(uri)
                        val newItem = AudioItemModel("${context?.getFileNameFromUri(uri)}")
                        audiosList.add(newItem)
                    }
                    else{
                        context?.showSmallLengthToast("You have already uploaded 3 audios")
                    }
                }

                //recycler view
                binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                val adapter = AudioItemAdapter(audiosList)
                binding.recyclerView.adapter = adapter

                val itemTouchHelper =
                    ItemTouchHelper(object : ItemTouchHelper.Callback() {
                        override fun getMovementFlags(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder
                        ): Int {
                            return makeMovementFlags(
                                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                                ItemTouchHelper.START
                            )
                        }

                        override fun onMove(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder
                        ): Boolean {


                            var sourceIndex = viewHolder.adapterPosition
                            var targetIndex = target.adapterPosition

                            Collections.swap(audiosList, sourceIndex, targetIndex)
                            Collections.swap(selectedUris, sourceIndex, targetIndex)

                            adapter.notifyItemMoved(sourceIndex, targetIndex)
                            binding.recyclerView.adapter = adapter

                            return false
                        }

                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            when (direction) {
                                ItemTouchHelper.START -> {
                                    audiosList.removeAt(viewHolder.adapterPosition)
                                    selectedUris.removeAt(viewHolder.adapterPosition)
                                    adapter.notifyItemRemoved(viewHolder.adapterPosition)
                                    binding.recyclerView.adapter = adapter

                                }

                            }

                        }
                    })

                itemTouchHelper.attachToRecyclerView(binding.recyclerView)


                removePreviousEdits()

                if(selectedUris.size>1){
                    pauseMediaPlayer()
                    savingDialog()
                    pathsList.clear()
                    mergeAudio(selectedUris)
                }
                else{
                    val path = context?.getInputPath(selectedUris[0])
                    audioUri = selectedUris[0]
                    outputFile = audioUri!!.path?.let { File(it) }
                    updatePathsList(outputFile!!.path)
                    createMediaPlayer(null, path)
                }
            }
        }
    }


    //***************************************** Dialogs  ***********************************************

    private fun savingDialog(progress: Int = 50) {
        // Update your progress UI or dialog here
        Log.d("AudioEditor", "Progress: $progress%")

        savingDialogBinding.progressBar.progress = 50
        savingDialogBinding.tvSaving.text = "Processing...(50%)"

        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = savingDialogBinding.root
        alertDialogBuilder?.setView(dialogView)?.setCancelable(false)
        savingAlertDialog = alertDialogBuilder?.create()
        savingDialogView = dialogView

        savingAlertDialog?.setOnDismissListener {
            dismissDialog(savingAlertDialog, savingDialogView)
        }

        savingAlertDialog?.show()
    }

    private fun showRenameDialog() {
        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = renameDialogBinding.root
        alertDialogBuilder?.setView(dialogView)
        renameAlertDialog = alertDialogBuilder?.create()

        renameDialogView = dialogView


        val filename = "audio_editor_${getCurrentTimestampString()}"

        renameDialogBinding.etRenameRD.setText(filename)
        renameDialogBinding.etRenameRD.setSelection(renameDialogBinding.etRenameRD.length())//placing cursor at the end of the text
        renameDialogBinding.etRenameRD.setSelectAllOnFocus(true)
        renameDialogBinding.etRenameRD.highlightColor = resources.getColor(R.color.thirtyPrecentAppBlue)
        renameDialogBinding.tvConfirmRD.setOnClickListener {
            context?.performHapticFeedback()
            // Handle the positive button click event here
            // You can retrieve the text entered in the EditText like this:
            val enteredText = renameDialogBinding.etRenameRD.text.toString()
            val name = enteredText.replaceSpaceWithUnderscore()


            if(name!=""){
                pauseMediaPlayer()
                moveFileToPublicDirectory(name)
                dismissDialog(renameAlertDialog, renameDialogView)
                savingDialog(50)
            }
            else{
                context?.showSmallLengthToast("Please write a valid name!")
            }
        }

        renameDialogBinding.tvCancelRD.setOnClickListener {
            context?.performHapticFeedback()
            // Handle the negative button click event here
            // This is where you can cancel the dialog if needed
            dismissDialog(renameAlertDialog, renameDialogView)

        }

        renameAlertDialog?.setOnDismissListener {
            dismissDialog(renameAlertDialog, renameDialogView)
        }

        renameAlertDialog!!.show()

    }

    private fun showQuitDialog() {

        var quitAlertDialog: AlertDialog? = null
        val quitDialogBinding by lazy {
            QuitDialogBinding.inflate(layoutInflater)
        }
        var quitDialogView: ConstraintLayout? = null


        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = quitDialogBinding.root
        alertDialogBuilder?.setView(dialogView)
        quitAlertDialog = alertDialogBuilder?.create()

        quitDialogView = dialogView


        quitDialogBinding.tvNo.setOnClickListener {
            context?.performHapticFeedback()
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitDialogBinding.tvYes.setOnClickListener {
            context?.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController().apply {
                if (currentDestination?.id == R.id.mergeAudiosFragment) {
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog?.setOnDismissListener {
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog!!.show()
    }


    private fun dialogDismiss() {
        Handler().postDelayed({
            dismissDialog(savingAlertDialog, savingDialogView)
        }, 1000)
    }


    //***************************************** Bottom Sheets  ***********************************************

    private fun openTrimBottomSheet() {



        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = trimBinding.root.parent as? ViewGroup
        parent?.removeView(trimBinding.root)
        bottomSheet.setContentView(trimBinding.root)
        trimBottomSheetShowing = true

        if(lastFunctionCalled == null){
            if(::mediaPlayer.isInitialized){
                mediaPlayer.release()
            }
            createMediaPlayer(audioUri, outputFile?.path)
            Log.d(TAG, "openTrimBottomSheet: null")
        }
        else{
            if(::mediaPlayer.isInitialized){
                mediaPlayer.release()
            }
            Log.d(TAG, "openTrimBottomSheet: !null")

            when(lastFunctionCalled){

                "trim" -> createMediaPlayer(audioUri, outputFile?.path)
                "fadein" -> createMediaPlayer(audioUri, outputPathFadeIn)
                "fadeout" -> createMediaPlayer(audioUri, outputPathFadeOut)

            }
        }


//        mediaPlayer.start()
        bottomSheet.setOnDismissListener {
            trimBottomSheetShowing = false
            if(::mediaPlayer.isInitialized){
                mediaPlayer.release()
                updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            }
            createMediaPlayer(audioUri, outputFile?.path)
        }

        trimBinding.cropWindowTrimBottomSheet.rangePositionChangeListener =
            object : RangeView.OnRangePositionListener {
                override fun leftTogglePositionChanged(xCoordinate: Float, value: Float) {
                    cropLeft = value
                    val audioDurationMillis = mediaPlayer.duration
                    val progress = value * 100
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
//                    binding.tvCropWindowLeft.text = selectedPositionMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())


                }

                override fun rightTogglePositionChanged(xCoordinate: Float, value: Float) {
                    cropRight = value
                    val audioDurationMillis = mediaPlayer.duration
                    val progress = (value * 100) - 5
                    val exactProgress = value * 100
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    val exactMillis = (exactProgress * audioDurationMillis) / 100
//                    binding.tvCropWindowRight.text = exactMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                }
            }

        trimBinding.waveformBottomSheet.onProgressChanged = object : SeekBarOnProgressChanged {
            override fun onProgressChanged(
                waveformSeekBar: WaveformSeekBar,
                progress: Float,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val audioDurationMillis = mediaPlayer.duration
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())
                }
            }
        }

        trimBinding.ivCancel.setOnOneClickListener {
            bottomSheet.dismiss()
            trimBottomSheetShowing = false
            if(::mediaPlayer.isInitialized){
                mediaPlayer.release()
                updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            }
            createMediaPlayer(audioUri, outputFile?.path)
        }

        trimBinding.ivDone.setOnOneClickListener {
            bottomSheet.dismiss()
            trim=true
            trimBottomSheetShowing = false
            mediaPlayer.pause()
            savingDialog(50)
            trimAudio()
        }



        bottomSheet.show()

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openFadeInBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = fadeInBinding.root.parent as? ViewGroup
        parent?.removeView(fadeInBinding.root)
        bottomSheet.setContentView(fadeInBinding.root)

        fadeInBinding.scrollRuler.scrollListener = object : ScrollRulerListener {
            override fun onRulerScrolled(value: Float) {
                fadeInBinding.tvNewValue.text = "${value}s"
                selectedFadeInTime = value.toString()
            }
        }

        fadeInBinding.ivValueDecrease.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start decreasing volume when button is pressed
                    startFadeInDecrease()
                }

                MotionEvent.ACTION_UP -> {
                    // Stop decreasing volume when button is released
                    stopValueUpdate()
                }
            }
            true
        }

        fadeInBinding.ivValueIncrease.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start increasing volume when button is pressed
                    startFadeInIncrease()
                }

                MotionEvent.ACTION_UP -> {
                    // Stop increasing volume when button is released
                    stopValueUpdate()
                }
            }
            true
        }

        fadeInBinding.btnDone.setOnOneClickListener {
            val durationInSeconds = mediaPlayer.duration / 1000

            if (selectedFadeInTime.toFloat().toInt() <= durationInSeconds) {
                if (currentFadeInTime != selectedFadeInTime) {
                    bottomSheet.dismiss()
                    changeFadeInTime()
                    savingDialog()
                } else {
                    context?.showSmallLengthToast("Selected fade in time is same as current fade in time")
                }
            } else {
                context?.showSmallLengthToast("Selected Fade in time is larger than audio duration")
            }
        }

        bottomSheet.show()

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openFadeOutBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = fadeOutBinding.root.parent as? ViewGroup
        parent?.removeView(fadeOutBinding.root)
        bottomSheet.setContentView(fadeOutBinding.root)

        fadeOutBinding.scrollRuler.scrollListener = object : ScrollRulerListener {
            override fun onRulerScrolled(value: Float) {
                fadeOutBinding.tvNewValue.text = "${value}s"
                selectedFadeOutTime = value.toString()
            }
        }

        fadeOutBinding.ivValueDecrease.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start decreasing volume when button is pressed
                    startFadeOutDecrease()
                }

                MotionEvent.ACTION_UP -> {
                    // Stop decreasing volume when button is released
                    stopValueUpdate()
                }
            }
            true
        }

        fadeOutBinding.ivValueIncrease.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start increasing volume when button is pressed
                    startFadeOutIncrease()
                }

                MotionEvent.ACTION_UP -> {
                    // Stop increasing volume when button is released
                    stopValueUpdate()
                }
            }
            true
        }

        fadeOutBinding.btnDone.setOnOneClickListener {

            val durationInSeconds = mediaPlayer.duration / 1000

            if (selectedFadeOutTime.toFloat().toInt() <= durationInSeconds) {
                if (currentFadeOutTime != selectedFadeOutTime) {
                    bottomSheet.dismiss()
                    changeFadeOutTime()
                    savingDialog()
                } else {
                    context?.showSmallLengthToast("Selected fade out time is same as current fade out time")
                }
            } else {
                context?.showSmallLengthToast("Selected Fade out time is larger than audio duration")
            }

        }


        bottomSheet.show()
    }


    //*****************************************   Bottom Sheet Functions  ***********************************************
    private fun startFadeInIncrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = fadeInBinding.scrollRuler.currentPositionValue
            Log.d(TAG, "startFadeInIncrease: $currentValue")
            if (currentValue <= 5) {

                var x = currentValue + 1f
                if(x>5){
                    x=5f
                }
                Log.d(TAG, "startFadeInIncrease: XXXTentacion $x")
                fadeInBinding.scrollRuler.scrollToValue(x)
                valueUpdateRunnable?.let {
                    valueUpdateHandler.postDelayed(
                        it,
                        100
                    )
                } // Adjust the delay as needed
            }
        }
        valueUpdateHandler.post(valueUpdateRunnable!!)
    }

    private fun startFadeInDecrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = fadeInBinding.scrollRuler.currentPositionValue
            Log.d(TAG, "startFadeInDecrease: $currentValue")
            if (currentValue >= 0.1) {
                fadeInBinding.scrollRuler.scrollToValue(currentValue - 0.1f)
                valueUpdateRunnable?.let {
                    valueUpdateHandler.postDelayed(
                        it,
                        100
                    )
                } // Adjust the delay as needed
            }
        }
        valueUpdateHandler.post(valueUpdateRunnable!!)
    }

    private fun startFadeOutIncrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = fadeOutBinding.scrollRuler.currentPositionValue
            Log.d(TAG, "startFadeOutIncrease: $currentValue")
            if (currentValue <= 5) {

                var x = currentValue + 1f
                if(x>5){
                    x=5f
                }
                Log.d(TAG, "startFadeInIncrease: XXXTentacion $x")
                fadeOutBinding.scrollRuler.scrollToValue(x)

                valueUpdateRunnable?.let {
                    valueUpdateHandler.postDelayed(
                        it,
                        100
                    )
                } // Adjust the delay as needed
            }
        }
        valueUpdateHandler.post(valueUpdateRunnable!!)
    }



    private fun startFadeOutDecrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = fadeOutBinding.scrollRuler.currentPositionValue
            if (currentValue >= 0.1) {
                fadeOutBinding.scrollRuler.scrollToValue(currentValue - 0.1f)
                valueUpdateRunnable?.let {
                    valueUpdateHandler.postDelayed(
                        it,
                        100
                    )
                } // Adjust the delay as needed
            }
        }
        valueUpdateHandler.post(valueUpdateRunnable!!)
    }


    private fun stopValueUpdate() {
        valueUpdateRunnable?.let {
            valueUpdateHandler.removeCallbacks(it)
            valueUpdateRunnable = null
        }
    }





    //***************************************** FFmpeg Functions  ***********************************************

    private fun mergeAudio(urisList: MutableList<Uri>) {

        outputFile = context?.getTemporaryFileInPrivateDirectory(extension)
        val endDurationsList = getEndDurations(urisList)

        val inputFiles = getInputFiles(urisList)
        val filterComplex = getFilterComplex(urisList, endDurationsList)

        val cmd = arrayOf(
            "-y",
            inputFiles,
            "-filter_complex",
            filterComplex,
            "-map", "[out]",
            outputFile!!.path
        )

        updatePathsList(outputFile!!.path)


        Log.e("AudioFile", "command: ${cmd.joinToString(" ")}")

        merge = true
        trim = false
        fadein = false
        fadeout = false

        cmd.executeCommand(this)

    }

    private fun trimAudio() {
        if(::mediaPlayer.isInitialized){
            if ( mediaPlayer.duration <= 0) {
                Log.e(TAG, "Invalid MediaPlayer state or duration")
                return
            }
            var durationInMillis = ((cropLeft) * (mediaPlayer.duration).toFloat()).toInt()
            val formattedDurationStart = durationInMillis.formatDuration()

            durationInMillis = ((cropRight) * (mediaPlayer.duration).toFloat()).toInt()
            val formattedDurationEnd = durationInMillis.formatDuration()

            context?.let {
                var inputAudioPath: String = ""
                if (lastFunctionCalled == null) {
                    inputAudioPath = audioUri?.let { it1 -> it.getInputPath(it1) }.toString()

                } else {
                    when (lastFunctionCalled) {
                        "fadein" -> inputAudioPath = outputPathFadeIn
                        "fadeout" -> inputAudioPath = outputPathFadeOut
                    }
                }

                val file = context?.getTemporaryFileInPrivateDirectory(extension)
                outputPathTrim = file!!.path

                val cmd = arrayOf(
                    "-y",//overwrite if exists
                    "-i",
                    pathsList[currentIndex],
                    "-ss",
                    formattedDurationStart,
                    "-to",
                    formattedDurationEnd,
                    outputPathTrim
                )

                Log.d(TAG, "trimAudio: ${cmd.joinToString(" ")}")


                merge = false
                trim = true
                fadein = false
                fadeout = false

                cmd.executeCommand(this)

            }
        }
    }

    private fun changeFadeInTime() {
        //ffmpeg -i input.mp3 -af "afade=t=in:ss=0:d=5" output.mp3

        val progress = cropLeft * 100
        val audioDurationMillis = mediaPlayer.duration
        val selectedPositionMillis = (progress * audioDurationMillis) / 100
        startingFadeInTime = (selectedPositionMillis / 1000)

        context?.let{
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(audioUri!!)
            }
            else{
                when(lastFunctionCalled){
                    "trim" -> inputAudioPath = outputPathTrim
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val file = context?.getTemporaryFileInPrivateDirectory(extension)
            outputPathFadeIn = file!!.path

            if(selectedFadeInTime.toFloat().toInt() > audioDurationMillis /1000){
                context?.showSmallLengthToast("Can't apply fadein")
                return
            }


            val cmd = arrayOf(
                "-y",
                "-i", pathsList[currentIndex],
                "-af", "afade=t=in:st=0:d=${selectedFadeInTime}",
                outputPathFadeIn
            )

            Log.d(TAG, "audioSpeed: ${cmd.joinToString(" ")}")

            merge = false
            trim = false
            fadein = true
            fadeout = false

            cmd.executeCommand(this)

        }
    }

    private fun changeFadeOutTime() {
        //ffmpeg -i input.mp3 -af "afade=t=in:ss=0:d=5" output.mp3

        val progress = cropRight * 100
        val audioDurationMillis = mediaPlayer.duration
        val selectedPositionMillis = (progress * audioDurationMillis) / 100
        startingFadeOutTime = (selectedPositionMillis / 1000)

        context?.let{
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(audioUri!!)
            }
            else{
                when(lastFunctionCalled){
                    "trim" -> inputAudioPath = outputPathTrim
                    "fadein" -> inputAudioPath = outputPathFadeIn
                }
            }

            val file = context?.getTemporaryFileInPrivateDirectory(extension)
            outputPathFadeOut = file!!.path

            val durationInSeconds = mediaPlayer.duration / 1000
            val startingTime = durationInSeconds - selectedFadeOutTime.toFloat().toInt()
            startingFadeOutTime -= selectedFadeOutTime.toFloat()

            startingFadeOutTime = ((durationInSeconds - selectedFadeOutTime.toFloat().toInt()).toFloat())
            if(startingFadeOutTime.toInt() > durationInSeconds){
                context?.showSmallLengthToast("Can't apply fadeout")
                return
            }

            val cmd = arrayOf(
                "-y",
                "-i", pathsList[currentIndex],
                "-af", "afade=t=out:st=${startingFadeOutTime}:d=${selectedFadeOutTime}",
                outputPathFadeOut
            )

            Log.d(TAG, "audioSpeed: ${cmd.joinToString(" ")}")

            merge = false
            trim = false
            fadein = false
            fadeout = true

            cmd.executeCommand(this)

        }
    }


    //***************************************** FFmpeg helper Functions  ***********************************************

     private fun getFilterComplex(
        urisList: MutableList<Uri>,
        endDurationsList: MutableList<Int>
    ): String {

        var noOfAudios = 0

        val filterComplex = StringBuilder()

        urisList.forEachIndexed { index, uri ->

            filterComplex.append("[$index:a]atrim=start=0:end=${endDurationsList[index]},asetpts=PTS-STARTPTS[a${index + 1}];")
            ++noOfAudios
        }

        val names = StringBuilder()
        urisList.forEachIndexed { index, uri ->
            names.append("[a${index + 1}]")
        }

        filterComplex.append("${names.toString()}concat=n=${noOfAudios}:v=0:a=1[out]")
        return filterComplex.toString()
    }

    override fun onCommandExecutionSuccess() {


        CoroutineScope(Dispatchers.IO).launch{
            if(merge){
                audioUri = Uri.fromFile(outputFile)
                pathsList.clear()
                updatePathsList(outputFile!!.path)
                createMediaPlayer(audioUri, outputFile?.path)
            }
            else if (trim) {
                audioUri = Uri.fromFile(File(outputPathTrim))
                lastFunctionCalled = "trim"
                updatePathsList(outputPathTrim)
                currentFadeInTime = "0.0"
                currentFadeOutTime = "0.0"
                createMediaPlayer(audioUri, outputPathTrim)
            } else if (fadein) {
                currentFadeInTime = selectedFadeInTime
                audioUri = Uri.fromFile(File(outputPathFadeIn))
                lastFunctionCalled = "fadein"
                updatePathsList(outputPathFadeIn)
                createMediaPlayer(audioUri, outputPathFadeIn)

//                updatePathsList(outputPathFadeIn)
            } else if (fadeout) {
                currentFadeOutTime = selectedFadeOutTime
                audioUri = Uri.fromFile(File(outputPathFadeOut))
                lastFunctionCalled = "fadeout"
                updatePathsList(outputPathFadeOut)
                createMediaPlayer(audioUri, outputPathFadeOut)

//                updatePathsList(outputPathFadeOut)
            }
        }
//        var outputPath = ""
//        if (lastFunctionCalled == null) {
//            outputPath = context?.getInputPath(audioUri!!).toString()
//        }
//        else{
//            when(lastFunctionCalled){
//                "trim" -> outputPath = outputPathTrim
//                "fadein" -> outputPath = outputPathFadeIn
//                "fadeout" -> outputPath = outputPathFadeOut
//            }
//        }
//
//        val bundle = Bundle().apply {
//            putString("AUDIO_FILEPATH", outputPath)
//        }
//
//        findNavController().apply {
//            if(currentDestination?.id == R.id.trimAudio){
//                navigate(R.id.action_trimAudio_to_savedScreenFragment, bundle)
//            }
//        }

    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "Processing Failed!"
        dialogDismiss()
    }

    //***************************************** Media Player Functions  ***********************************************
    private fun createMediaPlayer(uri: Uri? = null, path: String? = null) {

        if (::mediaPlayer.isInitialized) {
            // Release the previous MediaPlayer instance before creating a new one

            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer().apply {
            context?.let {
                if (uri != null) {
                    setDataSource(it, uri)
                }
                else if (path != null) {
                    setDataSource(it, Uri.parse(path))
                }


            }
            prepareAsync()

            setOnPreparedListener { mp ->
                if(!trimBottomSheetShowing){
                binding.waveform.visibility = View.VISIBLE
//                binding.cropWindowTrim.visibility = View.VISIBLE
//                binding.cropWindowTrim.setCurrentValues(0f, 1f)
//                cropLeft = 0f
//                cropRight = 1f
                if (uri != null) {
                    binding.waveform.setSampleFrom(uri)
                } else if (path != null) {
                    binding.waveform.setSampleFrom(Uri.fromFile(File(path)))
                }

                binding.waveform.waveWidth = 4F
                binding.waveform.maxProgress = 100F

                val durationMillis = mediaPlayer.duration
                binding.tvCurrentDuration.text = 0.formatDuration()
                Log.d(TAG, "createMediaPlayer: $durationMillis")
                binding.tvEndDuration.text = mediaPlayer.duration.formatDuration()


                binding.tvCurrentDurationWindow.text = 0.formatDuration()
                savingDialogBinding.progressBar.progress = 100
                savingDialogBinding.tvSaving.text = "Processing Done!"
                dialogDismiss()

                mp.setOnCompletionListener {
                    binding.waveform.progress = 0F
                    mediaPlayer.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.play_button)
                    binding.tvCurrentDuration.text = 0.formatDuration()

                }

            }
                else{
                trimBinding.cropWindowTrimBottomSheet.setCurrentValues(0F, 1F)
                cropLeft = 0F
                cropRight = 1F
                trimBinding.waveformBottomSheet.visibility = View.VISIBLE

                    if (uri != null) {
                        trimBinding.waveformBottomSheet.setSampleFrom(uri)
                    } else if (path != null) {
                        trimBinding.waveformBottomSheet.setSampleFrom(Uri.fromFile(File(path)))
                    }
                trimBinding.waveformBottomSheet.waveWidth = 4F
                trimBinding.waveformBottomSheet.maxProgress = 100F

//                    val durationMillis = mp.duration
//                    binding.tvCurrentDuration.text = mp.currentPosition.formatDuration()
//                    binding.tvAudioDuration.text = durationMillis.formatDuration()

                mediaPlayer.start()
                updateSeekBar()



                mp.setOnCompletionListener {
                    binding.waveform.progress = 0F
                    mediaPlayer.start()
//                        binding.btnPlayPause.setImageResource(R.drawable.pause_button)
                }
            }

            }
        }
    }

    private fun pauseMediaPlayer() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            binding.btnPlayPause.setImageResource(R.drawable.play_button)
        }
    }

    private fun updateSeekBar() {
        updateSeekBarHandler.postDelayed(updateSeekBarRunnable, 100)
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {

                if(!trimBottomSheetShowing){
                    val progress = mediaPlayer.calculateProgress()
                    binding.waveform.progress = progress
                    binding.tvCurrentDuration.text = mediaPlayer.currentPosition.formatDuration()
                    binding.tvCurrentDurationWindow.text = mediaPlayer.currentPosition.formatDuration()
                }
                else{
                    val progress = mediaPlayer.calculateProgress()
                    trimBinding.waveformBottomSheet.progress = progress
//                    if (progress >= mediaPlayer.duration) {
//                        // If at the end, start playing from the beginning
//                        mediaPlayer.seekTo(0)
//                        mediaPlayer.start()
//                    }
                    val audioDurationMillis = mediaPlayer.duration
                    val leftProgress = cropLeft * 100
                    val rightProgress = cropRight * 100

                    val leftPositionMillis = ((leftProgress * audioDurationMillis) / 100).toInt()
                    val rightPositionMillis = ((rightProgress * audioDurationMillis) / 100).toInt()

                    if(mediaPlayer.currentPosition >= rightPositionMillis){
                        mediaPlayer.seekTo(leftPositionMillis)
                    }
                    if(mediaPlayer.currentPosition < leftPositionMillis){
                        mediaPlayer.seekTo(leftPositionMillis)
                    }

                }

                // Update the SeekBar position every 100 milliseconds
                updateSeekBarHandler.postDelayed(this, 100)
            }
        }
    }

    //***************************************** Utility Functions  ***********************************************
    private fun getInputFiles(urisList: MutableList<Uri>): String {

        val inputFiles = StringBuilder()

        urisList.forEachIndexed { index, _ ->
            inputFiles.append("-i ${context?.getInputPath(urisList[index])} ")
//            context?.getInputPath(urisList[index])?.let { pathsList.add(it) }
        }

        return inputFiles.toString()

    }

    private fun getAudioDurationFromUri(uri: Uri): Int? {

        val mediaMetadataRetriever = MediaMetadataRetriever()

        try {
            // Set the data source for the media metadata retriever
            mediaMetadataRetriever.setDataSource(requireContext(), uri)

            // Get the duration of the audio in milliseconds
            val durationInMillisString =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            // Convert the duration to milliseconds and return
            return (durationInMillisString?.toInt())?.div(1000)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaMetadataRetriever.release()
        }

        return null
    }

    private fun getEndDurations(urisList: MutableList<Uri>): MutableList<Int> {

        val endDurationsList = mutableListOf<Int>()

        for (i in urisList) {
            val file = File(i.path!!)
            val x = getAudioDurationFromUri(i)
            Log.d(TAG, "getEndDurations: $i $x")
            if (x != null) {
                endDurationsList.add(x.toInt())
            }
        }
        return endDurationsList
    }

    private fun removePreviousEdits(){
        if(pathsList.size>1){
            context?.showSmallLengthToast("All previous edits removed")
            pathsList.clear()

            lastFunctionCalled = null
            audioUri= null
            outputFile= null

//            pathsListMutableLiveData.postValue(pathsList)
//            currentIndex= pathsList.lastIndex
//            audioUri = pathsList.last().getUriFromPath()
//            audioUri?.let{
//                createMediaPlayer(it)
//            }
            disableUndo()
            disableRedo()


            selectedFadeInTime = "0.0"
            selectedFadeOutTime = "0.0"

            currentFadeInTime = "0.0"
            currentFadeOutTime = "0.0"

        }
    }

    private fun enableUndo() {
        Log.d(TAG, "enableUndo: ")
        binding.btnUndo.setImageResource(R.drawable.ic_undo_enabled)
        binding.btnUndo.isClickable = true
        binding.btnUndo.isFocusable = true
    }

    private fun enableRedo() {
        Log.d(TAG, "enableRedo: ")
        binding.btnRedo.setImageResource(R.drawable.ic_redo_enabled)
        binding.btnRedo.isClickable = true
        binding.btnRedo.isFocusable = true

    }

    private fun disableUndo() {
        Log.d(TAG, "disableUndo: ")
        binding.btnUndo.setImageResource(R.drawable.ic_undo_disabled)
        binding.btnUndo.isClickable = false
        binding.btnUndo.isFocusable = false

    }

    private fun disableRedo() {
        Log.d(TAG, "disableRedo: ")

        binding.btnRedo.setImageResource(R.drawable.ic_redo_disabled)
        binding.btnRedo.isClickable = false
        binding.btnRedo.isFocusable = false

    }

    private fun updatePathsList(path: String){

        Log.d(TAG, "rightTogglePositionChanged: paths list $pathsList")

        if(pathsList.size>0){ pathsList.subList(currentIndex + 1, pathsList.size).clear() }

        pathsList.add(path)

        if(pathsList.size>4){
            pathsList.removeAt(1)
        }

        currentIndex = pathsList.lastIndex
//        pathsListMutableLiveData.postValue(pathsList)

        if(pathsList.size>1 && currentIndex>0){
            Handler(Looper.getMainLooper()).post {
                enableUndo()
            }
        }
        if(currentIndex==pathsList.size-1){
            Handler(Looper.getMainLooper()).post {
                disableRedo()
            }
        }


//        GlobalScope.launch {
//            createMediaPlayer(audioUri)
//        }

        Log.d(TAG, "rightTogglePositionChanged: paths list $pathsList")

    }

    private fun moveFileToPublicDirectory(name: String) {
        val file = pathsList[pathsList.lastIndex].moveFileFromPrivateToPublicDirectory(name)

        if(file != null){
            Log.d(TAG, "moveFileToPublicDirectory: true")
            savingDialogBinding.progressBar.progress = 100
            savingDialogBinding.tvSaving.text = "File Saved!"
            dialogDismiss()

            val metadata = file.getMetadataFromFile()

            Log.d(TAG, "moveFileToPublicDirectory: ${file.length()} ${file.extension} ${file.path.getAudioFileDuration()}")

            val bundle = Bundle().apply {
                putString("AUDIO_FILEPATH", file.path )
                putString("METADATA", metadata)

            }

            findNavController().apply {
                if(currentDestination?.id == R.id.mergeAudiosFragment){
                    navigate(R.id.action_mergeAudiosFragment_to_savedScreenFragment, bundle)
                }
            }
        }
        else{
            Log.d(TAG, "moveFileToPublicDirectory: false")
            savingDialogBinding.progressBar.progress = 0
            savingDialogBinding.tvSaving.text = "File Saving Failed!"
            dialogDismiss()
        }
    }




}







