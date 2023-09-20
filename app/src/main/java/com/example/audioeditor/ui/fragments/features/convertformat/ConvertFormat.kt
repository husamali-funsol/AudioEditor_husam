package com.example.audioeditor.ui.fragments.features.convertformat

import AudioFileContract
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.FragmentConvertFormatBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.ui.fragments.home.HomeFragment
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFilePath
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

    private lateinit var renameDialogBinding: RenameDialogBinding
    private var renameAlertDialog: AlertDialog? = null

    private var savingAlertDialog: AlertDialog? = null
    private lateinit var savingDialogBinding: SavingDialogBinding

    private var quitAlertDialog: AlertDialog? = null
    private lateinit var quitDialogBinding: QuitDialogBinding

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
            requireContext().performHapticFeedback()
        }

        binding.tvMp3.setOnOneClickListener {
            onTextViewClick(binding.tvMp3)
            requireContext().performHapticFeedback()

        }

        binding.tvWav.setOnOneClickListener {
            onTextViewClick(binding.tvWav)
            requireContext().performHapticFeedback()

        }

        binding.tvM4A.setOnOneClickListener {
            onTextViewClick(binding.tvM4A)
            requireContext().performHapticFeedback()

        }

        binding.tvFlac.setOnOneClickListener {
            onTextViewClick(binding.tvFlac)
            requireContext().performHapticFeedback()

        }

        binding.tvAac.setOnOneClickListener {
            onTextViewClick(binding.tvAac)
            requireContext().performHapticFeedback()

        }

        binding.tvOgg.setOnOneClickListener {
            onTextViewClick(binding.tvOgg)
            requireContext().performHapticFeedback()

        }

        binding.btnPlayPause.setOnOneClickListener {
            requireContext().performHapticFeedback()

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
            requireContext().performHapticFeedback()

            showRenameDialog()

//            convertFormat(audioUri!!)

        }

        binding.btnBack.setOnOneClickListener {
            showQuitDialog()
        }
    }

    private fun showQuitDialog() {
        val alertDialogBuilder =
            AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

        quitDialogBinding = QuitDialogBinding.inflate(layoutInflater)
        val dialogView = quitDialogBinding.root
        alertDialogBuilder.setView(dialogView)

        quitDialogBinding.tvNo.setOnClickListener {
            quitAlertDialog?.dismiss()
        }

        quitDialogBinding.tvYes.setOnClickListener {
            // Clear the back stack and navigate to the home fragment
            findNavController().popBackStack()
            findNavController().navigate(R.id.homeFragment)
            quitAlertDialog?.dismiss()
        }

        quitAlertDialog = alertDialogBuilder.create()
        quitAlertDialog!!.show()
    }

    private fun showRenameDialog() {
        val alertDialogBuilder =
            AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

        renameDialogBinding = RenameDialogBinding.inflate(layoutInflater)
        val dialogView = renameDialogBinding.root
        alertDialogBuilder.setView(dialogView)

        val filename = "audio_editor_${getCurrentTimestampString()}"

        renameDialogBinding.etRenameRD.setText(filename)
        renameDialogBinding.etRenameRD.setSelection(renameDialogBinding.etRenameRD.length())//placing cursor at the end of the text


        renameDialogBinding.tvConfirmRD.setOnClickListener {
            // Handle the positive button click event here
            // You can retrieve the text entered in the EditText like this:
            val enteredText = renameDialogBinding.etRenameRD.text.toString()
            val name = enteredText.replaceSpaceWithUnderscore()


            convertFormat(name, audioUri!!)

            handleProgress(50)
            renameAlertDialog?.dismiss()
        }

        renameDialogBinding.tvCancelRD.setOnClickListener {
            // Handle the negative button click event here
            // This is where you can cancel the dialog if needed
            renameAlertDialog?.dismiss()

        }

        renameAlertDialog = alertDialogBuilder.create()
        renameAlertDialog!!.show()

    }

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
            textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.textColorDarkGrey
                )
            )  // Set your desired text color here
        }

        selected = clickedTextView.text.toString()
        Log.d(TAG, "onTextViewClick: $selected")

        // Change the background color of the clicked TextView to blue
        clickedTextView.setBackgroundResource(R.drawable.button_bg)
        clickedTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
        )  // Set your desired text color here

    }

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)

            binding.tvMusicTitle.text = context?.getFileNameFromUri(uri)
        }
    }

    private fun createMediaPlayer(uri: Uri) {

        if (::mediaPlayer.isInitialized) {
            // Release the previous MediaPlayer instance before creating a new one

            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), uri)
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

    private fun convertFormat(filename: String, audioUri: Uri) {

        val inputAudioPath = requireContext().getInputPath(audioUri)
        val outputFile = filename.getOutputFilePath(selected)
        val outputPath = outputFile.path

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


    private fun handleProgress(progress: Int) {
        // Update your progress UI or dialog here
        Log.d("AudioEditor", "Progress: $progress%")

        val alertDialogBuilder =
            AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

        savingDialogBinding = SavingDialogBinding.inflate(layoutInflater)
        val dialogView = savingDialogBinding.root

        alertDialogBuilder.setView(dialogView)

//        savingDialogBinding.progressBar.progress = progress
//        savingDialogBinding.tvSaving.text  =progress.toString()

        savingAlertDialog = alertDialogBuilder.create()
        savingAlertDialog?.show()
    }

    private fun dismissDialog() {
        Handler().postDelayed({
            savingAlertDialog?.dismiss()
        }, 1000)    }

    override fun onCommandExecutionSuccess() {

        savingDialogBinding.progressBar.progress = 100
        savingDialogBinding.tvSaving.text = "File Saved!"
        dismissDialog()


    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "File Saving Failed!"
       dismissDialog()

    }

}

