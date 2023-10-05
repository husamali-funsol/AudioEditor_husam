package com.example.audioeditor.ui.fragments.features.recorder.editaudio

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.BottomSheetDetailsBinding
import com.example.audioeditor.databinding.BottomSheetSpeedBinding
import com.example.audioeditor.databinding.BottomSheetTrimBinding
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.FragmentEditAudioBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.lib.rangeview.RangeView
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFile
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles
import com.example.audioeditor.utils.setOnOneClickListener
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EditAudioFragment : Fragment(), CommandExecutionCallback {

    private val binding by lazy {
        FragmentEditAudioBinding.inflate(layoutInflater)
    }

    private val trimBinding by lazy {
        BottomSheetTrimBinding.inflate(layoutInflater)
    }

    private val renameDialogBinding by lazy {
        RenameDialogBinding.inflate(layoutInflater)
    }

    lateinit var mediaPlayer: MediaPlayer
    private val updateSeekBarHandler = Handler()

    private var alertDialog: AlertDialog? = null

    private var currentSpeedOption = "1"
    private var selectedSpeedOption = "-1"

    private var outputPath = ""
    private var cropLeft: Float = 0F
    private var cropRight: Float = 1F

    private var trimIn = false
    private var trimOut = false

    private var savingAlertDialog: AlertDialog? = null
    private val savingDialogBinding by lazy {
        SavingDialogBinding.inflate(layoutInflater)
    }
    private var savingDialogView: ConstraintLayout? = null

    private lateinit var audioUri: Uri

    private var outputPathSpeed = ""
    private var outputPathTrim = ""


    private var audioItem: LibraryItemModel? = null
    private var position: Int? = null

    private var speed = false
    private var trim = false
    private var trimBottomSheetShowing = false

    private var lastFunctionCalled: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        audioItem = arguments?.getParcelable("AUDIO_ITEM") as? LibraryItemModel
         position = arguments?.getInt("AUDIO_POSITION")

        setViews(audioItem)

        binding.btnPlayPause.setOnOneClickListener {
            context?.performHapticFeedback()
            if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                startMediaPlayer()

            } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                pauseMediaPlayer()
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


        binding.btnBack.setOnOneClickListener {
            showQuitDialog()
        }

        binding.btnMore.setOnOneClickListener {
            if(lastFunctionCalled==null){
                audioItem?.let {
                    position?.let { it1 ->
                        showMenu(it, it1, binding.btnMore)
                    }
                }
            }
            else{
                renameDialogBinding.tvConfirmRD.text = "Save"
                audioItem?.let {
                    position?.let { it1 ->
                        showRenameDialog(it, it1)
                    }
                }
            }
        }



        binding.viewSpeed.setOnOneClickListener {
            if (::mediaPlayer.isInitialized && binding.waveform.isVisible) {
                pauseMediaPlayer()
                openSpeedBottomSheet()
            }
        }

        binding.viewTrim.setOnOneClickListener {
            openTrimBottomSheet()
        }



    }

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
            createMediaPlayer(audioUri, audioItem?.path)
        }
        else{
            if(::mediaPlayer.isInitialized){
                mediaPlayer.release()
            }
            when(lastFunctionCalled){

                "speed" -> createMediaPlayer(audioUri, outputPathSpeed)
                "trim" -> createMediaPlayer(audioUri, outputPathTrim)

            }
        }


//        mediaPlayer.start()
        bottomSheet.setOnDismissListener {
            trimBottomSheetShowing = false
            if(::mediaPlayer.isInitialized){
                mediaPlayer.release()
                updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            }
            createMediaPlayer(audioUri, audioItem?.path)
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
            createMediaPlayer(audioUri, audioItem?.path)
        }

        trimBinding.ivDone.setOnOneClickListener {
            bottomSheet.dismiss()
            trim=true
            trimBottomSheetShowing = false
            mediaPlayer.pause()
            savingDialog(50)
            trimInAudio()
        }



        bottomSheet.show()

    }

    private fun onTextViewClick(speedBinding: BottomSheetSpeedBinding, clickedTextView: TextView) {
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


    private fun openSpeedBottomSheet() {

         val speedBinding = BottomSheetSpeedBinding.inflate(layoutInflater)


        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = speedBinding.root.parent as? ViewGroup
        parent?.removeView(speedBinding.root)
        bottomSheet.setContentView(speedBinding.root)
        with(speedBinding) {

            tv05x.setOnOneClickListener {
                onTextViewClick(speedBinding, tv05x)
            }

            tv075x.setOnOneClickListener {
                onTextViewClick(speedBinding , tv075x)
            }

            tv10x.setOnOneClickListener {
                onTextViewClick(speedBinding, tv10x)
            }

            tv125x.setOnOneClickListener {
                onTextViewClick(speedBinding, tv125x)
            }

            tv15x.setOnOneClickListener {
                onTextViewClick(speedBinding, tv15x)
            }

            tv20x.setOnOneClickListener {
                onTextViewClick(speedBinding, tv20x)
            }

            btnDone.setOnOneClickListener {
                if (selectedSpeedOption != "-1" && currentSpeedOption != selectedSpeedOption) {
                    changeSpeed()
                    speed = true
//                    changeMediaPlayerSpeed()
                    bottomSheet.dismiss()
                    savingDialog(50)
                } else {
                    context?.showSmallLengthToast("Selected speed is same as current speed")
                }
            }
        }
        bottomSheet.show()
    }

    private fun changeMediaPlayerSpeed() {
        if(::mediaPlayer.isInitialized){
            mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(selectedSpeedOption.toFloat())
        }

    }

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

    private fun changeSpeed() {

        context?.let{
            //audioitem.uri was giving error, file not found exception so used uri in this form.
//            val inputAudioPath = it.getInputPath(Uri.fromFile(audioItem?.path?.let { it1 -> File(it1) }))
            var inputAudioPath: String = ""
            if (lastFunctionCalled == null) {
                inputAudioPath = it.getInputPath(Uri.fromFile(audioItem?.path?.let { it1 -> File(it1) }))
            }
            else{
                when(lastFunctionCalled){
                    "speed" -> inputAudioPath = outputPathSpeed
                    "trim" -> inputAudioPath = outputPathTrim
                }
            }

            val outputFile = audioItem?.extension?.let {it2->
                "temp_audio_${getCurrentTimestampString()}".getOutputFile(it2)
            }
            outputPathSpeed = outputFile!!.path

            val cmd = arrayOf(
                "-y",
                "-i", inputAudioPath,
                "-filter_complex", "\"atempo=$selectedSpeedOption[aout]\"",
                "-map", "[aout]",
                outputPathSpeed
            )

            speed = true
            trim = false

            Log.d(TAG, "audioSpeed: ${cmd.joinToString(" ")}")

            cmd.executeCommand(this)

        }


    }


    private fun showMenu(libItem: LibraryItemModel, position: Int, iv: ImageView){
        val popupMenu = PopupMenu(requireContext(), iv)

        popupMenu.menuInflater.inflate(R.menu.audio_item_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->

            if(menuItem.title == "Rename"){
                renameDialogBinding.tvConfirmRD.text = "Confirm"
                showRenameDialog(libItem, position)
            }
            else if (menuItem.title == "Details"){
                showDetailsBottomSheet(libItem, position)
            }
            else if (menuItem.title == "Delete"){
                showDeleteDialog(libItem, position)
            }
            true
        }
        // Showing the popup menu
        popupMenu.show()
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
                if (currentDestination?.id == R.id.editAudio) {
                    popBackStack()
                    navigate(R.id.mainRecorderFragment)
                }
            }
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog?.setOnDismissListener {
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog!!.show()
    }


    private fun setViews(audioItem: LibraryItemModel?) {

        binding.tvFragmentTitle.text = audioItem?.title
        audioItem?.let {
            audioUri = it.uri!!
            createMediaPlayer(it.uri, it.path)
        }
    }

    private fun showRenameDialog(libItem: LibraryItemModel, position: Int) {

        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }
        val parent = renameDialogBinding.root.parent as? ViewGroup
        parent?.removeView(renameDialogBinding.root)
        val dialogView = renameDialogBinding.root
        alertDialogBuilder?.setView(dialogView)

        if (libItem != null) {
            // Set the initial text in your EditText (if needed)
            renameDialogBinding.etRenameRD.setText(libItem!!.title)
            renameDialogBinding.etRenameRD.setSelection(renameDialogBinding.etRenameRD.length())//placing cursor at the end of the text

        }

        renameDialogBinding.tvConfirmRD.setOnClickListener {
            // Handle the positive button click event here
            // You can retrieve the text entered in the EditText like this:
            val enteredText = renameDialogBinding.etRenameRD.text.toString()

            // Implement your logic here (e.g., renameFile(enteredText))
            val ext = libItem!!.extension
            Log.d("Debug", "entered text: $enteredText")
            Log.d("Debug", "ext: $ext")
            renameFile(enteredText, ext!!, libItem, position)

            alertDialog?.dismiss()
        }

        renameDialogBinding.tvCancelRD.setOnClickListener {
            // Handle the negative button click event here
            // This is where you can cancel the dialog if needed
            alertDialog?.dismiss()

        }

        alertDialog = alertDialogBuilder?.create()
        alertDialog!!.show()
    }

    private fun showDetailsBottomSheet(libItem: LibraryItemModel, position: Int) {
        val detailsBottomSheetDialogBinding by lazy {
            BottomSheetDetailsBinding.inflate(layoutInflater)
        }
        val detailsBottomSheet = BottomSheetDialog(requireContext())
        val parent = detailsBottomSheetDialogBinding.root.parent as? ViewGroup
        parent?.removeView(detailsBottomSheetDialogBinding.root)
        detailsBottomSheet.setContentView(detailsBottomSheetDialogBinding.root)

        if (libItem != null) {
            detailsBottomSheetDialogBinding.tvSetFilename.text = libItem!!.title
            detailsBottomSheetDialogBinding.tvSetTime.text = libItem!!.time
            detailsBottomSheetDialogBinding.tvSetPath.text = libItem!!.path
            detailsBottomSheetDialogBinding.tvSetSize.text = libItem!!.size
        }

        detailsBottomSheetDialogBinding.tvOkDetails.setOnClickListener {
            detailsBottomSheet.dismiss()
        }

        detailsBottomSheet.show()
    }

    private fun showDeleteDialog(libItem: LibraryItemModel, position: Int) {
        val deleteDialogBinding by lazy {
            DeleteDialogBinding.inflate(layoutInflater)
        }
        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }
        val parent = deleteDialogBinding.root.parent as? ViewGroup
        parent?.removeView(deleteDialogBinding.root)
        val dialogView = deleteDialogBinding.root
        alertDialogBuilder?.setView(dialogView)


        deleteDialogBinding.tvDeleteBtnDD.setOnClickListener {
            // Handle the positive button click event here
            val filePath =
                libItem.path
            val originalFile = File(filePath!!)

            if (originalFile.exists()) {
                originalFile.delete()
                context?.scanFiles(originalFile)

            }

            context?.refreshMediaStoreForAudioFiles()
            alertDialog?.dismiss()
            findNavController().popBackStack()
        }

        deleteDialogBinding.tvCancelDD.setOnClickListener {
            alertDialog?.dismiss()
        }

        alertDialog = alertDialogBuilder?.create()
        alertDialog!!.show()
    }

    private fun renameFile(newName: String, ext: String, libItem: LibraryItemModel, position: Int) {
        val filePath =
            libItem.path
        val newFileName = "$newName.$ext" // Provide the new file name
        val originalFile = File(filePath!!)
        // Create a File object for the new file with the desired name
        val directoryPath = originalFile.parentFile // Get the directory path
        val newFile = File(directoryPath, newFileName)

        // Rename the file
        if (originalFile.exists()) {
            if (originalFile.renameTo(newFile)) {

                val newPath =
                    newFile.path
                val updatedFile = File(newPath!!)

                val currentTimeMillis = System.currentTimeMillis()
                newFile.setLastModified(currentTimeMillis)
                originalFile.setLastModified(currentTimeMillis)
                libItem.time =  SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(
                    Date(currentTimeMillis)
                )
                // Refresh the MediaStore to reflect the changes
                context?.refreshMediaStore(updatedFile)
                context?.showSmallLengthToast("Renaming Successful")
//                adapter.itemUpdated(position, viewModel.getSingleFile(position))

            } else {
                // Failed to rename the file
                // Handle the error accordingly
                context?.showSmallLengthToast("Renaming Failed")
            }
        } else {
            // The original file does not exist
            context?.showSmallLengthToast("Original File doen not exist")
        }

        val newPath =
            newFile.path

        val updatedFile = File(newPath)
        libItem.path = newPath
        context?.refreshMediaStore(updatedFile)

        context?.refreshMediaStoreForAudioFiles()
//        getList()

        val fileNameWithExtension = updatedFile.name // This gives you "file.txt"
        binding.tvFragmentTitle.text = fileNameWithExtension.substringBeforeLast(".")
        libItem.title = fileNameWithExtension.substringBeforeLast(".")

    }

    private fun trimInAudio() {
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
                    inputAudioPath = it.getInputPath(Uri.fromFile(audioItem?.path?.let { it1 -> File(it1) }))

                } else {
                    when (lastFunctionCalled) {
                        "speed" -> inputAudioPath = outputPathSpeed
                        "trim" -> inputAudioPath = outputPathTrim
                    }
                }

                val outputFile = audioItem?.extension?.let { it2 ->
                    "temp_audio_${getCurrentTimestampString()}".getOutputFile(it2)
                }
                outputPathTrim = outputFile!!.path

                val cmd = arrayOf(
                    "-y",//overwrite if exists
                    "-i",
                    inputAudioPath,
                    "-ss",
                    formattedDurationStart,
                    "-to",
                    formattedDurationEnd,
                    outputPathTrim
                )

                Log.d(TAG, "trimInAudio: ${cmd.joinToString(" ")}")

                speed = false
                trim = true

                cmd.executeCommand(this)

            }
        }
    }



    private fun startMediaPlayer(){
        mediaPlayer.start()
        binding.btnPlayPause.setImageResource(R.drawable.pause_button)
        updateSeekBar()
    }

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
//                if (path != null) {
//                    setDataSource(path)
//                }


            }
            prepareAsync()

            setOnPreparedListener { mp ->
                if(!trimBottomSheetShowing){
                    binding.waveform.visibility = View.VISIBLE
                    binding.btnPlayPause.setImageResource(R.drawable.play_button)
                    binding.waveform.progress = 0F
                    if (path != null) {
                        binding.waveform.setSampleFrom(Uri.fromFile(File(path)))
                    }
                    binding.waveform.waveWidth = 4F
                    binding.waveform.maxProgress = 100F

                    val durationMillis = mp.duration
                    binding.tvCurrentDuration.text = mp.currentPosition.formatDuration()
                    binding.tvAudioDuration.text = durationMillis.formatDuration()


                    mp.setOnCompletionListener {
                        binding.waveform.progress = 0F
                        mediaPlayer.pause()
                        binding.btnPlayPause.setImageResource(R.drawable.play_button)
                    }
                }
                else{
                    trimBinding.cropWindowTrimBottomSheet.setCurrentValues(0F, 1F)
                    cropLeft = 0F
                    cropRight = 1F
                    trimBinding.waveformBottomSheet.visibility = View.VISIBLE
                    if (path != null) {
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

                    // Check if progress is at the end
//                    if (progress >= mediaPlayer.duration) {
//                        // If at the end, start playing from the beginning
//                        mediaPlayer.seekTo(0)
//                        mediaPlayer.start()
//                    }
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




    private fun pauseMediaPlayer() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            binding.btnPlayPause.setImageResource(R.drawable.play_button)
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

    private fun dialogDismiss() {
        Handler().postDelayed({
            dismissDialog(savingAlertDialog, savingDialogView)
        }, 1000)
    }

    override fun onCommandExecutionSuccess() {
        savingDialogBinding.progressBar.progress = 100
        savingDialogBinding.tvSaving.text = "Processing Done!"
        dialogDismiss()

        CoroutineScope(Dispatchers.IO).launch{
            if (speed) {
                currentSpeedOption = selectedSpeedOption
                audioUri = Uri.fromFile(File(outputPathSpeed))
                lastFunctionCalled = "speed"
                createMediaPlayer(audioUri, outputPathSpeed)
            }

            if (trim) {
                audioUri = Uri.fromFile(File(outputPathTrim))
                lastFunctionCalled = "trim"
                createMediaPlayer(audioUri, outputPathTrim)
            }
        }

        binding.btnMore.setImageResource(R.drawable.ic_tick)
    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "File Saving Failed!"
        dialogDismiss()
    }


}