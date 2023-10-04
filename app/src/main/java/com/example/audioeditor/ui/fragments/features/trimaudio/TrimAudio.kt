package com.example.audioeditor.ui.fragments.features.trimaudio

import AudioFileContract
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.BottomSheetFadeinBinding
import com.example.audioeditor.databinding.BottomSheetFadeoutBinding
import com.example.audioeditor.databinding.BottomSheetSpeedBinding
import com.example.audioeditor.databinding.BottomSheetVolumeBinding
import com.example.audioeditor.databinding.FragmentTrimAudioBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.lib.darioscrollruler.ScrollRulerListener
import com.example.audioeditor.lib.rangeview.RangeView
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.deleteFile
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.executeCommandInSeries
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getExtensionFromUri
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFile
import com.example.audioeditor.utils.getUriFromPath
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.replaceSpaceWithUnderscore
import com.example.audioeditor.utils.setOnOneClickListener
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class TrimAudio : Fragment(), CommandExecutionCallback {

    private val binding by lazy {
        FragmentTrimAudioBinding.inflate(layoutInflater)
    }

    private val volumeBinding by lazy {
        BottomSheetVolumeBinding.inflate(layoutInflater)
    }

    private val fadeInBinding by lazy {
        BottomSheetFadeinBinding.inflate(layoutInflater)
    }

    private val fadeOutBinding by lazy {
        BottomSheetFadeoutBinding.inflate(layoutInflater)
    }

    private val speedBinding by lazy {
        BottomSheetSpeedBinding.inflate(layoutInflater)
    }

    private var audioUri: Uri? = null

    private var selectedSpeedOption = "-1"
    private var selectedVolume = "1.00"
    private var selectedFadeInTime = "0.0"
    private var selectedFadeOutTime = "0.0"

    private var currentSpeedOption = "1"
    private var currentVolume = "1.00"
    private var currentFadeInTime = "0.0"
    private var currentFadeOutTime = "0.0"

    var startingFadeInTime = 0f
    var startingFadeOutTime = 0f

    private var extension: String? = null

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

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

    private var outputPath = ""
    private var cropLeft: Float = 0F
    private var cropRight: Float = 1F

    private var trimIn = false
    private var trimOut = false

    private val valueUpdateHandler = Handler()
    private var valueUpdateRunnable: Runnable? = null

    private var outputPathSpeed = ""
    private var outputPathVolume = ""
    private var outputPathFadeIn = ""
    private var outputPathFadeOut = ""

    private var speed = false
    private var volume = false
    private var fadein = false
    private var fadeout = false

    private var lastFunctionCalled: String? = null

    private var pathsList = mutableListOf<String>()

//    private var pathsListMutableLiveData = MutableLiveData<List<String>>()

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

        binding.btnRedo.isClickable=false
        binding.btnUndo.isClickable=false
        binding.btnUndo.isFocusable = false
        binding.btnRedo.isFocusable = false



//        pathsListMutableLiveData.observe(viewLifecycleOwner){
//            if(it.size > 1){
//                enableRedo()
//                enableUndo()
//            }
//            else{
//                disableRedo()
//                disableRedo()
//            }
//
//        }

        binding.btnUpload.setOnClickListener {
            audioFileLauncher.launch("audio/*")
            context?.performHapticFeedback()
        }

        binding.btnSave.setOnOneClickListener {
            if((trimIn || trimOut) && ::mediaPlayer.isInitialized){
                showRenameDialog()
            }
            else{
                context?.showSmallLengthToast("Please select Trimming type")
            }

        }

        binding.btnPlayPause.setOnOneClickListener {
            context?.performHapticFeedback()
            if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
                binding.btnPlayPause.setImageResource(R.drawable.pause_button)
                updateSeekBar()

            } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.btnPlayPause.setImageResource(R.drawable.play_button)
                updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            }
        }

        binding.waveform.onProgressChanged = object : SeekBarOnProgressChanged {
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

        binding.cropWindowTrim.rangePositionChangeListener =
            object : RangeView.OnRangePositionListener {
                override fun leftTogglePositionChanged(xCoordinate: Float, value: Float) {
                    cropLeft = value
                    val audioDurationMillis = mediaPlayer.duration
                    val progress = value * 100
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    binding.tvCropWindowLeft.text = selectedPositionMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                    removePreviousEdits()

                    Log.d(TAG, "rightTogglePositionChanged: paths list $pathsList")


                }

                override fun rightTogglePositionChanged(xCoordinate: Float, value: Float) {
                    cropRight = value
                    val audioDurationMillis = mediaPlayer.duration
                    val progress = (value * 100) - 3
                    val exactProgress = value * 100
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    val exactMillis = (exactProgress * audioDurationMillis) / 100
                    binding.tvCropWindowRight.text = exactMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                   removePreviousEdits()

                    Log.d(TAG, "rightTogglePositionChanged: paths list $pathsList")
                }
            }

        binding.cardTrimIn.setOnOneClickListener {
            if (::mediaPlayer.isInitialized) {
                if (!trimIn) {
                    binding.cardTrimOut.setBackgroundResource(R.drawable.button_bg_unselected)
                    binding.cardTrimIn.setBackgroundResource(R.drawable.button_bg_selected)
                    binding.ivTrimInIcon.setImageResource(R.drawable.ic_trim_in_selected)
                    binding.ivTrimOutIcon.setImageResource(R.drawable.ic_trim_out_unselected)
                    binding.tvTrimIn.setTextColor(resources.getColor(R.color.white))
                    binding.tvTrimOut.setTextColor(resources.getColor(R.color.textColorlightGrey))
                    binding.cropWindowTrim.visibility = View.VISIBLE
                    binding.tvCropWindowRight.text = mediaPlayer.duration.formatDuration()
                    binding.tvCropWindowLeft.text = 0.formatDuration()


                    trimIn = true
                    trimOut = false
                } else {
                    binding.cardTrimIn.setBackgroundResource(R.drawable.button_bg_unselected)
                    binding.ivTrimInIcon.setImageResource(R.drawable.ic_trim_in_unselected)
                    binding.tvTrimIn.setTextColor(resources.getColor(R.color.textColorlightGrey))
                    binding.cropWindowTrim.visibility = View.GONE
                    trimOut = false
                    trimIn = false
                }
            }
        }

        binding.cardTrimOut.setOnOneClickListener {
            if (::mediaPlayer.isInitialized) {

                if (!trimOut) {
                    binding.cardTrimIn.setBackgroundResource(R.drawable.button_bg_unselected)
                    binding.cardTrimOut.setBackgroundResource(R.drawable.button_bg_selected)
                    binding.ivTrimInIcon.setImageResource(R.drawable.ic_trim_in_unselected)
                    binding.ivTrimOutIcon.setImageResource(R.drawable.ic_trim_out_selected)
                    binding.tvTrimOut.setTextColor(resources.getColor(R.color.white))
                    binding.tvTrimIn.setTextColor(resources.getColor(R.color.textColorlightGrey))
                    cropLeft=0f
                    cropRight=1f
                    binding.cropWindowTrim.setCurrentValues(cropLeft, cropRight)
                    binding.cropWindowTrim.visibility = View.VISIBLE


                    binding.tvCropWindowRight.text = mediaPlayer.duration.formatDuration()
                    binding.tvCropWindowLeft.text = 0.formatDuration()

                    trimOut = true
                    trimIn = false
//                binding.cropWindowTrim.trimOutRangeView()
                } else {
                    binding.cardTrimOut.setBackgroundResource(R.drawable.button_bg_unselected)
                    binding.ivTrimOutIcon.setImageResource(R.drawable.ic_trim_out_unselected)
                    binding.tvTrimOut.setTextColor(resources.getColor(R.color.textColorlightGrey))
                    binding.cropWindowTrim.visibility = View.GONE
                    trimIn = false
                    trimOut = false
                }
            }
        }

        binding.viewVolume.setOnOneClickListener {
            if (::mediaPlayer.isInitialized && binding.waveform.isVisible && binding.cropWindowTrim.isVisible) {
                pauseMediaPlayer()
                openVolumeBottomSheet()
            }
        }

        binding.viewFadeIn.setOnOneClickListener {
            if (::mediaPlayer.isInitialized && binding.waveform.isVisible && binding.cropWindowTrim.isVisible) {
                pauseMediaPlayer()
                openFadeInBottomSheet()
            }
        }

        binding.viewFadeOut.setOnOneClickListener {
            if (::mediaPlayer.isInitialized && binding.waveform.isVisible && binding.cropWindowTrim.isVisible) {
                pauseMediaPlayer()
                openFadeOutBottomSheet()
            }
        }

        binding.viewSpeed.setOnOneClickListener {
            if (::mediaPlayer.isInitialized && binding.waveform.isVisible && binding.cropWindowTrim.isVisible) {
                pauseMediaPlayer()
                openSpeedBottomSheet()
            }
        }

        binding.ivMinusLeft.setOnClickListener {
            if (::mediaPlayer.isInitialized) {
                val audioDuration = mediaPlayer.duration / 1000
                val currentDuration = mediaPlayer.currentPosition / 1000
                val cropLeftProgress = cropLeft * 100
                val currentPosition = ((cropLeftProgress * (audioDuration*1000)) / 100)/1000
                var newCropLeft = (currentPosition.toFloat() - 1f) / audioDuration.toFloat()




                if (newCropLeft>0) {


                    cropLeft = newCropLeft
                    cropRight = binding.cropWindowTrim.getRightValue()
                    binding.cropWindowTrim.setCurrentValues(cropLeft, cropRight)

                    val audioDurationMillis = mediaPlayer.duration
                    val progress = cropLeft * 100
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    binding.tvCropWindowLeft.text = selectedPositionMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                    removePreviousEdits()
                }
            }
        }

        binding.ivAddLeft.setOnClickListener {
            if (::mediaPlayer.isInitialized) {
                val audioDuration = mediaPlayer.duration / 1000
                val currentDuration = mediaPlayer.currentPosition / 1000
                val cropLeftProgress = cropLeft * 100
                val currentPosition = ((cropLeftProgress * (audioDuration*1000)) / 100)/1000
                var newCropLeft = (currentPosition.toFloat() + 1f) / audioDuration.toFloat()


                if ( newCropLeft<cropRight) {


                    cropLeft = newCropLeft
                    cropRight = binding.cropWindowTrim.getRightValue()
                    binding.cropWindowTrim.setCurrentValues(cropLeft, cropRight)

                    val audioDurationMillis = mediaPlayer.duration
                    val progress = cropLeft * 100
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    binding.tvCropWindowLeft.text = selectedPositionMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                    removePreviousEdits()

                }
            }

        }

        binding.ivMinusRight.setOnClickListener {
            if (::mediaPlayer.isInitialized) {
                val audioDuration = mediaPlayer.duration / 1000
                var currentDuration = mediaPlayer.currentPosition / 1000
                var newCropRight = cropRight

                if (cropLeft < cropRight) {
                    val audioDurationMillis = mediaPlayer.duration
                    var progress = (cropRight * 100)
                    var selectedPositionMillis = (progress * audioDurationMillis) / 100
                    currentDuration = (selectedPositionMillis / 1000).toInt()
                    newCropRight = (currentDuration.toFloat() - 1f) / audioDuration.toFloat()

                    cropRight = newCropRight
                    cropLeft = binding.cropWindowTrim.getLeftValue()
                    binding.cropWindowTrim.setCurrentValues(cropLeft, cropRight)

                    progress = (cropRight * 100) - 3
                    val exactProgress = newCropRight * 100
                    selectedPositionMillis = (progress * audioDurationMillis) / 100
                    val exactMillis = (exactProgress * audioDurationMillis) / 100
                    binding.tvCropWindowRight.text = exactMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                    removePreviousEdits()

                }
            }

        }

        binding.ivAddRight.setOnClickListener {
            if (::mediaPlayer.isInitialized) {
                val audioDuration = mediaPlayer.duration / 1000
                var currentDuration = mediaPlayer.currentPosition / 1000
                var newCropRight = cropRight

                if (currentDuration <= audioDuration + 1 && cropRight < 1) {
                    val audioDurationMillis = mediaPlayer.duration
                    var progress = (cropRight * 100)
                    var selectedPositionMillis = (progress * audioDurationMillis) / 100
                    currentDuration = (selectedPositionMillis / 1000).toInt()
                    newCropRight = (currentDuration.toFloat() + 1f) / audioDuration.toFloat()


                    cropRight = newCropRight
                    cropLeft = binding.cropWindowTrim.getLeftValue()
                    binding.cropWindowTrim.setCurrentValues(cropLeft, cropRight)

                    progress = (cropRight * 100) - 3
                    val exactProgress = newCropRight * 100
                    selectedPositionMillis = (progress * audioDurationMillis) / 100
                    val exactMillis = (exactProgress * audioDurationMillis) / 100
                    binding.tvCropWindowRight.text = exactMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())

                    removePreviousEdits()

                }
            }
        }

        binding.btnBack.setOnOneClickListener {
            context?.performHapticFeedback()
            showQuitDialog()
        }

        binding.btnUndo.setOnOneClickListener{
            Log.d(TAG, "onViewCreated: undo ${binding.btnUndo.isClickable}")
            Log.d(TAG, "onViewCreated: undo ${currentIndex}")

            if(currentIndex>0 && pathsList.size>0){
                Log.d(TAG, "onViewCreated: undo if")

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
        binding.btnRedo.setOnOneClickListener{
            Log.d(TAG, "onViewCreated: redo ${binding.btnRedo.isClickable}")
            Log.d(TAG, "onViewCreated: redo ${currentIndex}")

            if(currentIndex<pathsList.size-1){
                Log.d(TAG, "onViewCreated: redo if")

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



    }



    override fun onDestroy() {
        super.onDestroy()

        if(::mediaPlayer.isInitialized){
            mediaPlayer.release()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }

    override fun onPause() {
        super.onPause()

        if(::mediaPlayer.isInitialized && mediaPlayer.isPlaying){
            mediaPlayer.pause()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }

    override fun onResume() {
        super.onResume()

        if(::mediaPlayer.isInitialized){
            mediaPlayer.start()
            updateSeekBar()
        }
    }



    //***************************************** Audio Upload ***********************************************

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

//            createMediaPlayer(audioUri!!)


            context?.getInputPath(audioUri!!)?.let {
                currentIndex = 0
                updatePathsList(it)

            }
//            setMetadata(audioUri!!)
            extension = context?.getExtensionFromUri(uri)
            binding.tvMusicTitle.text = context?.getFileNameFromUri(uri)
        }
    }

    //***************************************** Dialogs ***********************************************

    private fun savingDialog(progress: Int = 50) {
        // Update your progress UI or dialog here
        Log.d("AudioEditor", "Progress: $progress%")

        savingDialogBinding.progressBar.progress = 50
        savingDialogBinding.tvSaving.text = "Saving...(50%)"

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

    private fun showQuitDialog() {
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
                if (currentDestination?.id == R.id.trimAudio) {
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

        renameDialogBinding.tvConfirmRD.setOnClickListener {
            context?.performHapticFeedback()
            // Handle the positive button click event here
            // You can retrieve the text entered in the EditText like this:
            val enteredText = renameDialogBinding.etRenameRD.text.toString()
            val name = enteredText.replaceSpaceWithUnderscore()

            if(trimIn){
                pauseMediaPlayer()
                trimInAudio()
            }
            else if (trimOut){
                pauseMediaPlayer()
                trimOutAudio()
            }
            dismissDialog(renameAlertDialog, renameDialogView)

            savingDialog(50)
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


    private fun dialogDismiss() {
        Handler().postDelayed({
            dismissDialog(savingAlertDialog, savingDialogView)
        }, 1000)


    }

    //*****************************************   Media Player and Waveform  ***********************************************

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
                if (path != null) {
                    setDataSource(path)
                }


            }
            prepareAsync()

            setOnPreparedListener { mp ->
                binding.waveform.visibility = View.VISIBLE
                if (uri != null) {
                    binding.waveform.setSampleFrom(uri)
                }
                binding.waveform.waveWidth = 4F
                binding.waveform.maxProgress = 100F

                val durationMillis = mp.duration
                binding.tvCurrentDuration.text = mp.currentPosition.formatDuration()
                binding.tvEndDuration.text = durationMillis.formatDuration()
                binding.tvCropWindowLeft.text = mp.currentPosition.formatDuration()
                binding.tvCropWindowRight.text = durationMillis.formatDuration()

                mp.setOnCompletionListener {
                    binding.waveform.progress = 0F
                    mediaPlayer.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.play_button)
                }

            }
        }
    }

    private fun updateSeekBar() {
        updateSeekBarHandler.postDelayed(updateSeekBarRunnable, 100)
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                val progress = mediaPlayer.calculateProgress()
                binding.waveform.progress = progress
                binding.tvCurrentDuration.text = mediaPlayer.currentPosition.formatDuration()

                // Check if progress is at the end
                if (progress >= mediaPlayer.duration) {
                    // If at the end, start playing from the beginning
                    mediaPlayer.seekTo(0)
                    mediaPlayer.start()
                }

                // Update the SeekBar position every 100 milliseconds
                updateSeekBarHandler.postDelayed(this, 100)
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

    //*****************************************   Bottom Sheets  ***********************************************
    @SuppressLint("ClickableViewAccessibility")
    private fun openVolumeBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = volumeBinding.root.parent as? ViewGroup
        parent?.removeView(volumeBinding.root)
        bottomSheet.setContentView(volumeBinding.root)

        volumeBinding.scrollRuler.scrollListener = object : ScrollRulerListener {
            override fun onRulerScrolled(value: Float) {
                val formattedValue = if (value % 1 == 0f) {
                    "${value.toInt()}"
                } else {
                    "${value}"
                }
                volumeBinding.tvNewValue.text = "${formattedValue}%"
                selectedVolume = String.format("%.2f", (formattedValue.toFloat() / 100f))
            }
        }

        volumeBinding.ivValueDecrease.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start decreasing volume when button is pressed
                    startVolumeDecrease()
                }

                MotionEvent.ACTION_UP -> {
                    // Stop decreasing volume when button is released
                    stopValueUpdate()
                }
            }
            true
        }

        volumeBinding.ivValueIncrease.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Start increasing volume when button is pressed
                    startVolumeIncrease()
                }

                MotionEvent.ACTION_UP -> {
                    // Stop increasing volume when button is released
                    stopValueUpdate()
                }
            }
            true
        }

        volumeBinding.btnDone.setOnOneClickListener {
            if (currentVolume != selectedVolume) {
                bottomSheet.dismiss()
                changeVolume()

                savingDialog(50)
            } else {
                context?.showSmallLengthToast("Selected volume is same as current volume")
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

    private fun openSpeedBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = speedBinding.root.parent as? ViewGroup
        parent?.removeView(speedBinding.root)
        bottomSheet.setContentView(speedBinding.root)
        with(speedBinding) {

            tv05x.setOnOneClickListener {
                onTextViewClick(tv05x)
            }

            tv075x.setOnOneClickListener {
                onTextViewClick(tv075x)
            }

            tv10x.setOnOneClickListener {
                onTextViewClick(tv10x)
            }

            tv125x.setOnOneClickListener {
                onTextViewClick(tv125x)
            }

            tv15x.setOnOneClickListener {
                onTextViewClick(tv15x)
            }

            tv20x.setOnOneClickListener {
                onTextViewClick(tv20x)
            }

            btnDone.setOnOneClickListener {
                if (selectedSpeedOption != "-1" && currentSpeedOption != selectedSpeedOption) {
                    changeSpeed()
                    bottomSheet.dismiss()
                    savingDialog(50)
                } else {
                    context?.showSmallLengthToast("Selected speed is same as current speed")
                }
            }
        }
        bottomSheet.show()
    }

    //*****************************************   Bottom Sheet Functions  ***********************************************
    private fun startFadeInIncrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = fadeInBinding.scrollRuler.currentPositionValue
            if (currentValue <= 4.9) {
                fadeInBinding.scrollRuler.scrollToValue(currentValue + 0.1f)
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
            if (currentValue <= 4.9) {
                fadeOutBinding.scrollRuler.scrollToValue(currentValue + 0.1f)
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

    private fun startVolumeDecrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = volumeBinding.scrollRuler.currentPositionValue
            if (currentValue >= 51) {
                volumeBinding.scrollRuler.scrollToValue(currentValue - 1f)
                valueUpdateRunnable?.let {
                    valueUpdateHandler.postDelayed(
                        it,
                        100
                    )
                } // Adjust the delay as needed
            } else if (currentValue > 50) {
                volumeBinding.scrollRuler.scrollToValue(0f)
            }
        }
        valueUpdateHandler.post(valueUpdateRunnable!!)
    }

    private fun startVolumeIncrease() {
        valueUpdateRunnable = Runnable {
            val currentValue = volumeBinding.scrollRuler.currentPositionValue
            if (currentValue <= 199) {
                volumeBinding.scrollRuler.scrollToValue(currentValue + 1f)
                valueUpdateRunnable?.let {
                    valueUpdateHandler.postDelayed(
                        it,
                        100
                    )
                } // Adjust the delay as needed
            } else if (currentValue < 200) {
                volumeBinding.scrollRuler.scrollToValue(100f)
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


    //*****************************************   FFmpeg Functions  ***********************************************

    private fun trimInAudio() {
        var durationInMillis = ((cropLeft) * (mediaPlayer.duration).toFloat()).toInt()
        val formattedDurationStart = durationInMillis.formatDuration()

        durationInMillis = ((cropRight) * (mediaPlayer.duration).toFloat()).toInt()
        val formattedDurationEnd = durationInMillis.formatDuration()

        context?.let {
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(audioUri!!)

            } else {
                when (lastFunctionCalled) {
                    "speed" -> inputAudioPath = outputPathSpeed
                    "volume" -> inputAudioPath = outputPathVolume
                    "fadein" -> inputAudioPath = outputPathFadeIn
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val outputFile = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            outputPath = outputFile!!.path

            val cmd = arrayOf(
                "-y",//overwrite if exists
                "-i",
                pathsList[currentIndex],
                "-ss",
                formattedDurationStart,
                "-to",
                formattedDurationEnd,
                outputPath
            )

            cmd.executeCommand(this)

        }
    }

    private fun trimOutAudio() {

        var durationInMillis = ((cropLeft) * (mediaPlayer.duration).toFloat()).toInt()
        val formattedDurationStart = durationInMillis.formatDuration()

        durationInMillis = ((cropRight) * (mediaPlayer.duration).toFloat()).toInt()
        val formattedDurationEnd = durationInMillis.formatDuration()

        val formattedStartPointAudio = 0.formatDuration() //start of audio
        val formattedAudioDuration = (mediaPlayer.duration).formatDuration() //total audio duration


        context?.let {
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(audioUri!!)

            } else {
                when (lastFunctionCalled) {
                    "speed" -> inputAudioPath = outputPathSpeed
                    "volume" -> inputAudioPath = outputPathVolume
                    "fadein" -> inputAudioPath = outputPathFadeIn
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val outputFile1 = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            val outputPath1 = outputFile1!!.path

            val outputFile2 = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            val outputPath2 = outputFile2!!.path

            val outputFile = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            outputPath = outputFile!!.path

            val cmd1 = arrayOf(
                "-y",//overwrite if exists
                "-i",
                pathsList[currentIndex],
                "-ss",
                formattedStartPointAudio,
                "-to",
                formattedDurationStart,
                outputPath1
            )

            val cmd2 = arrayOf(
                "-y",//overwrite if exists
                "-i",
                pathsList[currentIndex],
                "-ss",
                formattedDurationEnd,
                "-to",
                formattedAudioDuration,
                outputFile2.path
            )

            val cmd3 = arrayOf(
                "-y",
                "-i", outputPath1,
                "-i", outputPath2,
                "-filter_complex",
                "[0:a][1:a]concat=n=2:v=0:a=1[out]",
                "-map", "[out]",
                outputPath
            )

            cmd1.executeCommandInSeries() { success1 ->
                if (success1) {
                    // cmd1 was successful, proceed to cmd2
                    cmd2.executeCommandInSeries() { success2 ->
                        if (success2) {
                            // cmd2 was successful, proceed to cmd3
                            cmd3.executeCommandInSeries() { success3 ->
                                if (success3) {
                                    // cmd3 was successful
                                    deleteFile(outputFile1)
                                    deleteFile(outputFile2)
                                } else {
                                    Log.d("Audiotrim", "cmd3 failed")
                                }
                            }
                        } else {
                            Log.d("Audiotrim", "cmd2 failed")
                        }
                    }
                } else {
                    Log.d("Audiotrim", "cmd1 failed")
                }
            }
        }
    }

    private fun changeVolume() {
        //ffmpeg -i input.mp3 -af "volume=2.0" output.mp3

        context?.let{
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(audioUri!!)
            }
            else{
                when(lastFunctionCalled){
                    "speed" -> inputAudioPath = outputPathSpeed
                    "volume" -> inputAudioPath = outputPathVolume
                    "fadein" -> inputAudioPath = outputPathFadeIn
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val outputFile = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            outputPathVolume = outputFile!!.path

            val cmd = arrayOf(
                "-y",
                "-i", pathsList[currentIndex],
                "-af", "volume=$selectedVolume",
                outputPathVolume
            )

            Log.d(TAG, "changeVolume: ${cmd.joinToString(" ")}")
            volume=true
            speed=false
            fadein=false
            fadeout=false

            cmd.executeCommand(this)

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
                    "speed" -> inputAudioPath = outputPathSpeed
                    "volume" -> inputAudioPath = outputPathVolume
                    "fadein" -> inputAudioPath = outputPathFadeIn
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val outputFile = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            outputPathFadeIn = outputFile!!.path


            val cmd = arrayOf(
                "-y",
                "-i", pathsList[currentIndex],
                "-af", "afade=t=in:ss=${startingFadeInTime}:d=${selectedFadeInTime}",
                outputPathFadeIn
            )

            Log.d(TAG, "audioSpeed: ${cmd.joinToString(" ")}")

            speed = false
            volume = false
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
                    "speed" -> inputAudioPath = outputPathSpeed
                    "volume" -> inputAudioPath = outputPathVolume
                    "fadein" -> inputAudioPath = outputPathFadeIn
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val outputFile = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            outputPathFadeOut = outputFile!!.path

            val durationInSeconds = mediaPlayer.duration / 1000
            val startingTime = durationInSeconds - selectedFadeOutTime.toFloat().toInt()
            startingFadeOutTime -= selectedFadeOutTime.toFloat()

            val cmd = arrayOf(
                "-y",
                "-i", pathsList[currentIndex],
                "-af", "afade=t=out:st=${startingFadeOutTime}:d=${selectedFadeOutTime}",
                outputPathFadeOut
            )

            Log.d(TAG, "audioSpeed: ${cmd.joinToString(" ")}")

            speed = false
            volume = false
            fadein = false
            fadeout = true

            cmd.executeCommand(this)

        }
    }

    private fun changeSpeed() {

        context?.let{
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(audioUri!!)
            }
            else{
                when(lastFunctionCalled){
                    "speed" -> inputAudioPath = outputPathSpeed
                    "volume" -> inputAudioPath = outputPathVolume
                    "fadein" -> inputAudioPath = outputPathFadeIn
                    "fadeout" -> inputAudioPath = outputPathFadeOut
                }
            }

            val outputFile = extension?.let {
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it)
            }
            outputPathSpeed = outputFile!!.path

            val cmd = arrayOf(
                "-y",
                "-i", pathsList[currentIndex],
                "-filter_complex", "\"atempo=$selectedSpeedOption[aout]\"",
                "-map", "[aout]",
                outputPathSpeed
            )

            Log.d(TAG, "audioSpeed: ${cmd.joinToString(" ")}")

            speed = true
            volume = false
            fadein = false
            fadeout = false

            Log.d(TAG, "changeSpeed: $lastFunctionCalled")
            Log.d(TAG, "changeSpeed: ${cmd.joinToString(" ")}")


            cmd.executeCommand(this)

        }


    }


    //*****************************************   FFmpeg Execution Results  ***********************************************
    override fun onCommandExecutionSuccess() {
        savingDialogBinding.progressBar.progress = 100
        savingDialogBinding.tvSaving.text = "File Saved!"
        dialogDismiss()

        CoroutineScope(Dispatchers.IO).launch{
            if (speed) {
                currentSpeedOption = selectedSpeedOption
                audioUri = Uri.fromFile(File(outputPathSpeed))
                lastFunctionCalled = "speed"
                updatePathsList(outputPathSpeed)
            } else if (volume) {
                currentVolume = selectedVolume
                audioUri = Uri.fromFile(File(outputPathVolume))
                lastFunctionCalled = "volume"
                updatePathsList(outputPathVolume)
            } else if (fadein) {
                currentFadeInTime = selectedFadeInTime
                audioUri = Uri.fromFile(File(outputPathFadeIn))
                lastFunctionCalled = "fadein"
                updatePathsList(outputPathFadeIn)
            } else if (fadeout) {
                currentFadeOutTime = selectedFadeOutTime
                audioUri = Uri.fromFile(File(outputPathFadeOut))
                lastFunctionCalled = "fadeout"
                updatePathsList(outputPathFadeOut)
            }

        }
    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "File Saving Failed!"
        dialogDismiss()
    }

    //*****************************************   Utility Functions  ***********************************************

    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(
            speedBinding.tv05x,
            speedBinding.tv075x,
            speedBinding.tv10x,
            speedBinding.tv125x,
            speedBinding.tv15x,
            speedBinding.tv20x
        )
        for (textView in allTextViews) {

            context?.let {
                textView.setBackgroundResource(R.drawable.button_bg_grey_rounded)
                textView.setTextColor(
                    ContextCompat.getColor(
                        it,
                        R.color.textColorDarkGrey
                    )
                )
            }
        }

        val text = clickedTextView.text.toString()
        selectedSpeedOption = text.removeSuffix("x")
        Log.d(TAG, "onTextViewClick: $selectedSpeedOption")
        // Change the background color of the clicked TextView to blue
        clickedTextView.setBackgroundResource(R.drawable.button_bg_rounded)
        context?.let {
            clickedTextView.setTextColor(
                ContextCompat.getColor(
                    it,
                    R.color.white
                )
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
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


        GlobalScope.launch {
            createMediaPlayer(audioUri)
        }

        Log.d(TAG, "rightTogglePositionChanged: paths list $pathsList")

    }

    private fun removePreviousEdits(){
        if(pathsList.size>1){
            context?.showSmallLengthToast("All previous edits removed")
            pathsList.subList(1, pathsList.size).clear()
            disableRedo()
            disableUndo()
//            pathsListMutableLiveData.postValue(pathsList)
            currentIndex= pathsList.lastIndex
            audioUri = pathsList.last().getUriFromPath()
            audioUri?.let{
                createMediaPlayer(it)
            }
            disableUndo()
            disableRedo()

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

}