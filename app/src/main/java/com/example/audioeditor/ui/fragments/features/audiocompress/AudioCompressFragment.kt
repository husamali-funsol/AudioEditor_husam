package com.example.audioeditor.ui.fragments.features.audiocompress

import AudioFileContract
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.FragmentAudioCompressBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.formatSizeToMB
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getExtensionFromUri
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFile
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.replaceSpaceWithUnderscore
import com.example.audioeditor.utils.setOnOneClickListener
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class AudioCompressFragment : Fragment(), CommandExecutionCallback {

    private val binding by lazy {
        FragmentAudioCompressBinding.inflate(layoutInflater)
    }

    private var audioUri: Uri? = null

    private var selectedAudioQuality = "LOW"
    private var selectedSampleRate = "8000"
    private var selectedBitrate = "64"

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
    private val quitDialogBinding by lazy{
        QuitDialogBinding.inflate(layoutInflater)
    }
    private var quitDialogView: ConstraintLayout? = null


    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var updatedText = ""

    private var extension: String? = null

    private var pathsList = listOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.btnUpload.setOnClickListener {
            audioFileLauncher.launch("audio/*")
            context?.performHapticFeedback()
        }


        //audio quality selection
        binding.tvLowQuality.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvLowQuality, "AudioQuality")
            onTextViewClick(binding.tv8khz, "SampleRate")
            onTextViewClick(binding.tv64kbs, "Bitrate")


            selectedAudioQuality = "low"
            selectedSampleRate = "8000"
            selectedBitrate = "64"

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tvMidQuality.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvMidQuality, "AudioQuality")
            onTextViewClick(binding.tv22khz, "SampleRate")
            onTextViewClick(binding.tv192kbs, "Bitrate")

            selectedAudioQuality = "mid"
            selectedSampleRate = "22050"
            selectedBitrate = "192"

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tvHighQuality.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvHighQuality, "AudioQuality")
            onTextViewClick(binding.tv48khz, "SampleRate")
            onTextViewClick(binding.tv320kbs, "Bitrate")

            selectedAudioQuality = "high"
            selectedSampleRate = "48000"
            selectedBitrate = "320"

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }


        //sample rate selection
        binding.tv48khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv48khz, "SampleRate")
            selectedSampleRate = "48000"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv44khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv44khz, "SampleRate")
            selectedSampleRate = "44100"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv32khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv32khz, "SampleRate")
            selectedSampleRate = "32000"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv22khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv22khz, "SampleRate")
            selectedSampleRate = "22050"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv16khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv16khz, "SampleRate")
            selectedSampleRate = "16000"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv11khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv11khz, "SampleRate")
            selectedSampleRate = "11025"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv8khz.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv8khz, "SampleRate")
            selectedSampleRate = "8000"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }



        //bitrate selection
        binding.tv320kbs.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv320kbs, "Bitrate")
            selectedBitrate = "320"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv256kbs.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv256kbs, "Bitrate")
            selectedBitrate = "256"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv192kbs.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv192kbs, "Bitrate")
            selectedBitrate = "192"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv128kbs.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv128kbs, "Bitrate")
            selectedBitrate = "128"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv96kbs.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv96kbs, "Bitrate")
            selectedBitrate = "96"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }

        binding.tv64kbs.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv64kbs, "Bitrate")
            selectedBitrate = "64"
            unselectAudioQuality()

            audioUri?.let{
                updateEstimatedFilesizeText(it)
            }
        }


        //save button
        binding.btnSave.setOnOneClickListener {
            context?.performHapticFeedback()
            showRenameDialog()

        }

        //back button
        binding.btnBack.setOnOneClickListener {
            context?.performHapticFeedback()
            showQuitDialog()
        }


        binding.btnPlayPause.setOnOneClickListener {
            context?.performHapticFeedback()

            if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                updateSeekBar()

            } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
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
    }

    //***************************************** Upload Audio ***********************************************
    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)

            binding.tvMusicTitle.text = context?.getFileNameFromUri(uri)
            extension = context?.getExtensionFromUri(uri)
            updateEstimatedFilesizeText(uri)

        }
    }

    //***************************************** MediaPlayer Functions ***********************************************
    private fun createMediaPlayer(uri: Uri) {

        if (::mediaPlayer.isInitialized) {
            // Release the previous MediaPlayer instance before creating a new one

            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer().apply {
            context?.let{ setDataSource(it, uri) }
            prepareAsync()

            setOnPreparedListener { mp ->
                binding.waveform.setSampleFrom(uri)
                binding.waveform.waveWidth = 4F
                binding.waveform.maxProgress = 100F

                mp.setOnCompletionListener {
                    binding.waveform.progress = 0F
                    mediaPlayer.start()
                    // Reset progress to 0 when audio completes
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

    //***************************************** FFmpeg Command ***********************************************
    private fun audioCompress(filename: String, audioUri: Uri){
        context?.let{
            val inputAudioPath = it.getInputPath(audioUri)
            val outputFile = extension?.let { filename.getOutputFile(it) }
            val outputPath = outputFile!!.path

            val cmd = arrayOf(
                "-y",
                "-i",
                inputAudioPath,
                "-b:a",
                "${selectedBitrate}k",
                "-ar", selectedSampleRate,
                "-map",
                "a",
                outputPath
            )

            Log.d(TAG, "audioCompress: ${cmd.joinToString(" ")}")

            cmd.executeCommand(this)
        }
    }


    //***************************************** FFmpeg Callback Functions ***********************************************
    override fun onCommandExecutionSuccess() {

        savingDialogBinding.progressBar.progress = 100
        savingDialogBinding.tvSaving.text = "File Saved!"
        dialogDismiss()

        val bundle = Bundle().apply {
            putString("AUDIO_URI", audioUri.toString())
        }

        findNavController().apply {
            if(currentDestination?.id == R.id.audioCompress){
                navigate(R.id.action_audioCompress_to_savedScreenFragment, bundle)
            }
        }

    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "File Saving Failed!"
        dialogDismiss()

    }


    //***************************************** Utility Functions ***********************************************
    private fun getAudioFileDuration(uri: Uri): Long {
        val mediaPlayer = MediaPlayer()


        return try {
            context?.let{ mediaPlayer.setDataSource(it, uri) }
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong()
            mediaPlayer.release()  // Release MediaPlayer after obtaining duration
            duration
        } catch (e: IOException) {
            // Log the error or handle it as needed
            mediaPlayer.release()  // Make sure to release the MediaPlayer on error
            0
        }
    }

    private fun dialogDismiss() {
        Handler().postDelayed({
            dismissDialog(savingAlertDialog, savingDialogView)
        }, 1000)
    }

    private fun onTextViewClick(clickedTextView: TextView, cardName: String) {
        // Reset all TextViews to white
        var allTextViews: List<TextView> = listOf()
        when(cardName){
            "AudioQuality" -> {
                allTextViews = listOf(
                    binding.tvLowQuality,
                    binding.tvMidQuality,
                    binding.tvHighQuality
                )
            }
            "SampleRate" -> {
                allTextViews = listOf(
                    binding.tv48khz,
                    binding.tv44khz,
                    binding.tv32khz,
                    binding.tv22khz,
                    binding.tv16khz,
                    binding.tv11khz,
                    binding.tv8khz
                )
            }
            "Bitrate" -> {
                allTextViews = listOf(
                    binding.tv320kbs,
                    binding.tv256kbs,
                    binding.tv192kbs,
                    binding.tv128kbs,
                    binding.tv96kbs,
                    binding.tv64kbs
                )
            }

        }

        for (textView in allTextViews) {
            if(cardName=="AudioQuality"){
                textView.setBackgroundResource(R.drawable.button_outlined)
                context?.let{
                    textView.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.appBlue
                        )
                    )
                }
            }
            else{
                context?.let{
                    textView.setBackgroundColor(
                        ContextCompat.getColor(
                            it,
                            R.color.transparent
                        )
                    )
                    textView.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.textColorlightGrey
                        )
                    )
                }
            }
        }
        clickedTextView.setBackgroundResource(R.drawable.button_bg)
        context?.let{
            clickedTextView.setTextColor(
                ContextCompat.getColor(
                    it,
                    R.color.white
                )
            )
        }

    }

    private fun unselectAudioQuality(){
        when(selectedAudioQuality.lowercase()){
            "low" -> {
                binding.tvLowQuality.setBackgroundResource(R.drawable.button_outlined)
                context?.let{
                    binding.tvLowQuality.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.appBlue
                        )
                    )
                }
            }
            "mid" -> {
                binding.tvMidQuality.setBackgroundResource(R.drawable.button_outlined)
                context?.let{
                    binding.tvMidQuality.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.appBlue
                        )
                    )
                }
            }
            "high" -> {
                binding.tvHighQuality.setBackgroundResource(R.drawable.button_outlined)
                context?.let{
                    binding.tvHighQuality.setTextColor(
                        ContextCompat.getColor(
                            it,
                            R.color.appBlue
                        )
                    )
                }
            }
        }

    }

    private fun updateEstimatedFilesizeText(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val duration = getAudioFileDuration(uri) / 1000L
            val bitrate = selectedBitrate.toLong() * 1000
            val filesize = estimateFileSizeAfterCompression(duration, bitrate).formatSizeToMB()

            // Switch to the main thread to update the UI
            withContext(Dispatchers.Main) {
                // Update the UI
                Log.d(TAG, "duration: $duration \n bitrate: $bitrate \n filesize: $filesize ")
                updatedText = "Size will be about $filesize MB after compression"
                binding.tvEstimateSize.text = updatedText
                binding.tvEstimateSize.visibility = View.VISIBLE
            }
        }
    }

    private fun estimateFileSizeAfterCompression(audioDuration: Long, targetBitrate: Long): Long{
        //Estimated Compressed File Size = (Target Bitrate * Audio Duration in Seconds) / 8
        //file size will be in bytes
        return (targetBitrate * audioDuration) / 8
    }


    //***************************************** Dialogs ***********************************************
    private fun showQuitDialog() {
        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = quitDialogBinding.root
        quitDialogView = dialogView
        alertDialogBuilder?.setView(dialogView)
        quitAlertDialog = alertDialogBuilder?.create()



        quitDialogBinding.tvNo.setOnClickListener {
            context?.performHapticFeedback()
            dismissDialog(quitAlertDialog, quitDialogView)

        }

        quitDialogBinding.tvYes.setOnClickListener {
            context?.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController().apply{
                if(currentDestination?.id == R.id.audioCompress)
                {
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            dismissDialog(quitAlertDialog, quitDialogView)

        }

        quitAlertDialog?.setOnDismissListener {
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog?.show()
    }

    private fun showRenameDialog() {
        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        var dialogView = renameDialogBinding.root
        renameDialogView = dialogView
        alertDialogBuilder?.setView(dialogView)
        renameAlertDialog = alertDialogBuilder?.create()

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

            audioCompress(name, audioUri!!)
            dismissDialog(renameAlertDialog, renameDialogView)
            savingDialog()

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


    private fun savingDialog(progress: Int = 50) {
        // Update your progress UI or dialog here
        Log.d("AudioEditor", "Progress: $progress%")

        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = savingDialogBinding.root
        savingDialogView = dialogView
        alertDialogBuilder?.setView(dialogView)?.setCancelable(false)
        savingAlertDialog = alertDialogBuilder?.create()

        savingAlertDialog?.setOnDismissListener {
            dismissDialog(savingAlertDialog, savingDialogView)
        }

        savingAlertDialog?.show()
    }

    //***************************************** Override Functions ***********************************************
    override fun onPause() {
        super.onPause()

        if(::mediaPlayer.isInitialized && mediaPlayer.isPlaying){
            mediaPlayer.pause()
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if(::mediaPlayer.isInitialized){
            mediaPlayer.release()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)

        }
    }


}