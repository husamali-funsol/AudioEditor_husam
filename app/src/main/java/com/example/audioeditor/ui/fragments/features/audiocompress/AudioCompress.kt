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
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getExtensionFromUri
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFilePath
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.replaceSpaceWithUnderscore
import com.example.audioeditor.utils.setOnOneClickListener
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import java.util.Locale


class AudioCompress : Fragment(), CommandExecutionCallback {

    private lateinit var binding: FragmentAudioCompressBinding

    private var audioUri: Uri? = null

    private var selectedAudioQuality = "LOW"
    private var selectedSampleRate = "8000"
    private var selectedBitrate = "64k"

    private lateinit var renameDialogBinding: RenameDialogBinding
    private var renameAlertDialog: AlertDialog? = null

    private var savingAlertDialog: AlertDialog? = null
    private lateinit var savingDialogBinding: SavingDialogBinding

    private var quitAlertDialog: AlertDialog? = null
    private lateinit var quitDialogBinding: QuitDialogBinding

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var extension: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAudioCompressBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUpload.setOnClickListener {
            audioFileLauncher.launch("audio/*")
            requireContext().performHapticFeedback()
        }


        //audio quality selection
        binding.tvLowQuality.setOnOneClickListener {
            onTextViewClick(binding.tvLowQuality, "AudioQuality")
            onTextViewClick(binding.tv8khz, "SampleRate")
            onTextViewClick(binding.tv64kbs, "Bitrate")

            selectedAudioQuality = "low"
            selectedSampleRate = "8000"
            selectedBitrate = "64k"
        }

        binding.tvMidQuality.setOnOneClickListener {
            onTextViewClick(binding.tvMidQuality, "AudioQuality")
            onTextViewClick(binding.tv22khz, "SampleRate")
            onTextViewClick(binding.tv192kbs, "Bitrate")

            selectedAudioQuality = "mid"
            selectedSampleRate = "22000"
            selectedBitrate = "192k"
        }

        binding.tvHighQuality.setOnOneClickListener {
            onTextViewClick(binding.tvHighQuality, "AudioQuality")
            onTextViewClick(binding.tv48khz, "SampleRate")
            onTextViewClick(binding.tv320kbs, "Bitrate")

            selectedAudioQuality = "high"
            selectedSampleRate = "48000"
            selectedBitrate = "320k"
        }


        //sample rate selection
        binding.tv48khz.setOnOneClickListener {
            onTextViewClick(binding.tv48khz, "SampleRate")
            selectedSampleRate = "48000"
            unselectAudioQuality()
        }

        binding.tv44khz.setOnOneClickListener {
            onTextViewClick(binding.tv44khz, "SampleRate")
            selectedSampleRate = "44000"
            unselectAudioQuality()
        }

        binding.tv32khz.setOnOneClickListener {
            onTextViewClick(binding.tv32khz, "SampleRate")
            selectedSampleRate = "32000"
            unselectAudioQuality()
        }

        binding.tv22khz.setOnOneClickListener {
            onTextViewClick(binding.tv22khz, "SampleRate")
            selectedSampleRate = "22000"
            unselectAudioQuality()
        }

        binding.tv16khz.setOnOneClickListener {
            onTextViewClick(binding.tv16khz, "SampleRate")
            selectedSampleRate = "16000"
            unselectAudioQuality()
        }

        binding.tv11khz.setOnOneClickListener {
            onTextViewClick(binding.tv11khz, "SampleRate")
            selectedSampleRate = "11000"
            unselectAudioQuality()
        }

        binding.tv8khz.setOnOneClickListener {
            onTextViewClick(binding.tv8khz, "SampleRate")
            selectedSampleRate = "8000"
            unselectAudioQuality()
        }



        //bitrate selection
        binding.tv320kbs.setOnOneClickListener {
            onTextViewClick(binding.tv320kbs, "Bitrate")
            selectedBitrate = "320k"
            unselectAudioQuality()
        }

        binding.tv256kbs.setOnOneClickListener {
            onTextViewClick(binding.tv256kbs, "Bitrate")
            selectedBitrate = "256k"
            unselectAudioQuality()
        }

        binding.tv192kbs.setOnOneClickListener {
            onTextViewClick(binding.tv192kbs, "Bitrate")
            selectedBitrate = "192k"
            unselectAudioQuality()
        }

        binding.tv128kbs.setOnOneClickListener {
            onTextViewClick(binding.tv128kbs, "Bitrate")
            selectedBitrate = "128k"
            unselectAudioQuality()
        }

        binding.tv96kbs.setOnOneClickListener {
            onTextViewClick(binding.tv96kbs, "Bitrate")
            selectedBitrate = "96k"
            unselectAudioQuality()
        }

        binding.tv64kbs.setOnOneClickListener {
            onTextViewClick(binding.tv64kbs, "Bitrate")
            selectedBitrate = "64k"
            unselectAudioQuality()
        }


        //save button
        binding.btnSave.setOnOneClickListener {
            requireContext().performHapticFeedback()

            showRenameDialog()

        }

        //back button
        binding.btnBack.setOnOneClickListener {
            showQuitDialog()
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
    }

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)

            binding.tvMusicTitle.text = context?.getFileNameFromUri(uri)
            extension = context?.getExtensionFromUri(uri)
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

            audioCompress(name, audioUri!!)

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


    private fun unselectAudioQuality(){
        when(selectedAudioQuality.lowercase()){
            "low" -> {
                binding.tvLowQuality.setBackgroundResource(R.drawable.button_outlined)
                binding.tvLowQuality.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.appBlue
                    )
                )
            }
            "mid" -> {
                binding.tvMidQuality.setBackgroundResource(R.drawable.button_outlined)
                binding.tvMidQuality.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.appBlue
                    )
                )
            }
            "high" -> {
                binding.tvHighQuality.setBackgroundResource(R.drawable.button_outlined)
                binding.tvHighQuality.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.appBlue
                    )
                )
            }
        }

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
                textView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.appBlue
                    )
                )
            }
            else{
                textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transparent))
                textView.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.textColorlightGrey
                    )
                )
            }
        }
        clickedTextView.setBackgroundResource(R.drawable.button_bg)
        clickedTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
        )

    }

    private fun audioCompress(filename: String, audioUri: Uri){
        val inputAudioPath = requireContext().getInputPath(audioUri)
        val outputFile = extension?.let { filename.getOutputFilePath(it) }
        val outputPath = outputFile!!.path

        val cmd = arrayOf(
            "-y",
            "-i",
            inputAudioPath,
            "-b:a",
            selectedBitrate,
            "-ar", selectedSampleRate,
            "-map",
            "a",
            outputPath
        )

        Log.d(TAG, "audioCompress: ${cmd.joinToString  (" ") }")

        cmd.executeCommand(this)
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