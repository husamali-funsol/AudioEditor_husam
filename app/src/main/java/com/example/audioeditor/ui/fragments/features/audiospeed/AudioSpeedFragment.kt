package com.example.audioeditor.ui.fragments.features.audiospeed

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
import com.example.audioeditor.databinding.FragmentAudioSpeedBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
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

class AudioSpeedFragment : Fragment(), CommandExecutionCallback {

    private val binding by lazy {
        FragmentAudioSpeedBinding.inflate(layoutInflater)
    }

    private var selectedOption = "-1"
    private var audioUri: Uri? = null
    private val updateSeekBarHandler = Handler()
    private lateinit var mediaPlayer: MediaPlayer

    private var extension: String? = null

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



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply{

            if(selectedOption.toFloat()>-1f){
                when(selectedOption){
                    "0.5" -> onTextViewClick(tv05x)
                    "0.75" -> onTextViewClick(tv075x)
                    "1.0" -> onTextViewClick(tv10x)
                    "1.25" -> onTextViewClick(tv125x)
                    "1.5" -> onTextViewClick(tv15x)
                    "2.0" -> onTextViewClick(tv20x)
                }
            }

             btnBack.setOnOneClickListener {
                context?.performHapticFeedback()
                showQuitDialog()
            }

             btnSave.setOnOneClickListener {
                context?.performHapticFeedback()
                showRenameDialog()
            }

             tv05x.setOnOneClickListener {
                context?.performHapticFeedback()
                onTextViewClick( tv05x)
            }

             tv075x.setOnOneClickListener {
                context?.performHapticFeedback()
                onTextViewClick( tv075x)
            }

             tv10x.setOnOneClickListener {
                context?.performHapticFeedback()
                onTextViewClick( tv10x)
            }

             tv125x.setOnOneClickListener {
                context?.performHapticFeedback()
                onTextViewClick( tv125x)
            }

             tv15x.setOnOneClickListener {
                context?.performHapticFeedback()
                onTextViewClick( tv15x)
            }

             tv20x.setOnOneClickListener {
                context?.performHapticFeedback()
                onTextViewClick( tv20x)
            }

             btnUpload.setOnClickListener {
                audioFileLauncher.launch("audio/*")
                context?.performHapticFeedback()
            }

            btnPlayPause.setOnOneClickListener {
                context?.performHapticFeedback()
                if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                    updateSeekBar()

                } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                    updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
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
                        mediaPlayer.seekTo(selectedPositionMillis.toInt())
                    }
                }
            }
        }
    }

    //***************************************** Audio Upload  ***********************************************

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)
            extension = context?.getExtensionFromUri(uri)
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

    //***************************************** Utility Functions  ***********************************************

    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(
            binding.tv05x,
            binding.tv075x,
            binding.tv10x,
            binding.tv125x,
            binding.tv15x,
            binding.tv20x
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

        val text = clickedTextView.text.toString()
        selectedOption = text.removeSuffix("x")
        Log.d(TAG, "onTextViewClick: $selectedOption")

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

    //***************************************** FFmpeg Functions  ***********************************************

    private fun audioSpeed(filename: String, audioUri: Uri) {

        context?.let{
            val inputAudioPath = it.getInputPath(audioUri)
            val outputFile = extension?.let {filename.getOutputFile(it)}
            val outputPath = outputFile!!.path

            val cmd = arrayOf(
                "-y",
                "-i", inputAudioPath,
                "-filter_complex", "\"atempo=$selectedOption[aout]\"",
                "-map", "[aout]",
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
            putString("AUDIO_URI", audioUri.toString())
        }

        findNavController().apply {
            if(currentDestination?.id == R.id.audioSpeed){
                navigate(R.id.action_audioSpeed_to_savedScreenFragment, bundle)
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
                if(currentDestination?.id == R.id.audioSpeed ){
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

            audioSpeed(name, audioUri!!)
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

    private fun savingDialog(progress: Int = 50) {
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

    //***************************************** Override Functions  ***********************************************

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