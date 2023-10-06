package com.example.audioeditor.ui.fragments.features.convertformat

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
import com.example.audioeditor.databinding.FragmentConvertFormatBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFile
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.replaceSpaceWithUnderscore
import com.example.audioeditor.utils.setOnOneClickListener
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar


class ConvertFormat : Fragment(), CommandExecutionCallback {


    private val binding by lazy {
        FragmentConvertFormatBinding.inflate(layoutInflater)
    }

    private var audioUri: Uri? = null

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var selected = "mp3"

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

    private var outputPath = ""


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

        binding.tvMp3.setOnOneClickListener {
            onTextViewClick(binding.tvMp3)
            context?.performHapticFeedback()
        }

        binding.tvWav.setOnOneClickListener {
            onTextViewClick(binding.tvWav)
            context?.performHapticFeedback()
        }

        binding.tvM4A.setOnOneClickListener {
            onTextViewClick(binding.tvM4A)
            context?.performHapticFeedback()
        }

        binding.tvFlac.setOnOneClickListener {
            onTextViewClick(binding.tvFlac)
            context?.performHapticFeedback()
        }

        binding.tvAac.setOnOneClickListener {
            onTextViewClick(binding.tvAac)
            context?.performHapticFeedback()
        }

        binding.tvOgg.setOnOneClickListener {
            onTextViewClick(binding.tvOgg)
            context?.performHapticFeedback()
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

        binding.btnSave.setOnOneClickListener {
            context?.performHapticFeedback()
            showRenameDialog()
        }

        binding.btnBack.setOnOneClickListener {
            context?.performHapticFeedback()
            showQuitDialog()
        }
    }


    //***************************************** MAudio Upload ***********************************************

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)

            binding.tvMusicTitle.text = context?.getFileNameFromUri(uri)
        }
    }

    //***************************************** Media Player Functions  ***********************************************

    private fun createMediaPlayer(uri: Uri) {

        if (::mediaPlayer.isInitialized) {
            // Release the previous MediaPlayer instance before creating a new one

            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer().apply {
            context?.let{
                setDataSource(it, uri)
            }
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

    //***************************************** FFmpeg Functions  ***********************************************

    private fun convertFormat(filename: String, audioUri: Uri) {

        context?.let{
            val inputAudioPath = it.getInputPath(audioUri)
            val outputFile = filename.getOutputFile(selected)
            outputPath = outputFile.path


            //ffmpeg -i input_file.ext output_file.ext

            //to m4a
            //ffmpeg -i input_file.mp3 -c:a aac -b:a 256k output_file.m4a

//        if(ext == "m4a") {
//            val cmd = arrayOf(
//                "-y",
//                "-i", inputAudioPath,
//                "-c:a", "aac",
//                "-b:a", "256k",
//                outputPath
//            )
//        }
//        else {
//            val cmd = arrayOf(
//                "-y",
//                "-i", inputAudioPath,
//                outputPath
//            )
//        }

            //problem whil econverting to m4a===========================
            //retry it again with above commands.==================================


            val cmd = arrayOf(
                "-y",
                "-i", inputAudioPath,
                outputPath
            )

            cmd.executeCommand(this)
        }


    }

    override fun onCommandExecutionSuccess() {

        savingDialogBinding.progressBar.progress = 100
        savingDialogBinding.tvSaving.text = "File Saved!"
        dismissDialog()

        val bundle = Bundle().apply {
            putString("AUDIO_URI", outputPath)
        }

        findNavController().apply {
            if(currentDestination?.id == R.id.convertFormat){
                navigate(R.id.action_convertFormat_to_savedScreenFragment, bundle)
            }
        }


    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "File Saving Failed!"
        dismissDialog()

    }

    //***************************************** Dialogs  ***********************************************

    private fun showQuitDialog() {
        val alertDialogBuilder =
            context?.let{
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
            findNavController().apply{
                if(currentDestination?.id == R.id.convertFormat ){
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

            convertFormat(name, audioUri!!)
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

    private fun savingDialog(progress: Int =50) {
        // Update your progress UI or dialog here
        Log.d("AudioEditor", "Progress: $progress%")

        val alertDialogBuilder =
            context?.let{
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

    private fun dismissDialog() {
        Handler().postDelayed({
            dismissDialog(savingAlertDialog, savingDialogView)
        }, 1000)    }

    //***************************************** Utility Functions  ***********************************************

    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(
            binding.tvMp3,
            binding.tvWav,
            binding.tvM4A,
            binding.tvFlac,
            binding.tvAac,
            binding.tvOgg
        )
        for (textView in allTextViews) {

            context?.let{
                textView.setBackgroundResource(R.drawable.button_bg_white)
                textView.setTextColor(
                    ContextCompat.getColor(
                        it,
                        R.color.textColorDarkGrey
                    )
                )
            }

        }

        selected = clickedTextView.text.toString()
        Log.d(TAG, "onTextViewClick: $selected")

        // Change the background color of the clicked TextView to blue
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

    //***************************************** Overrride Functions  ***********************************************

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
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }





}

