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
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentConvertFormatBinding
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFilePath
import com.example.audioeditor.utils.scanFiles
import com.example.audioeditor.utils.setOnOneClickListener
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar

var TAG = "Hello"
class ConvertFormat : Fragment() {


    private lateinit var binding: FragmentConvertFormatBinding

    private var audioUri: Uri? = null

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var selected = "mp3"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentConvertFormatBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUpload.setOnClickListener {
            audioFileLauncher.launch("audio/*")
        }

        binding.tvMp3.setOnOneClickListener {
            onTextViewClick(binding.tvMp3)
        }

        binding.tvWav.setOnOneClickListener {
            onTextViewClick(binding.tvWav)
        }

        binding.tvM4A.setOnOneClickListener {
            onTextViewClick(binding.tvM4A)
        }

        binding.tvFlac.setOnOneClickListener {
            onTextViewClick(binding.tvFlac)
        }

        binding.tvAac.setOnOneClickListener {
            onTextViewClick(binding.tvAac)
        }

        binding.tvOgg.setOnOneClickListener {
            onTextViewClick(binding.tvOgg)
        }

        binding.btnPlayPause.setOnOneClickListener {
            if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                mediaPlayer.start()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                updateSeekBar()
            }
            else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
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

        }
    }


    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(binding.tvMp3, binding.tvWav, binding.tvM4A, binding.tvFlac, binding.tvAac, binding.tvOgg)
        for (textView in allTextViews) {
            textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorDarkGrey))  // Set your desired text color here
        }

        selected = clickedTextView.text.toString()
        Log.d(TAG, "onTextViewClick: $selected")

        // Change the background color of the clicked TextView to blue
        clickedTextView.setBackgroundResource(R.drawable.button_bg)
        clickedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))  // Set your desired text color here

    }

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)

            binding.tvMusicTitle.text = getFileNameFromUri(uri)
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

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex =
                    it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex).substringBeforeLast(".")
                }
            }
        }
        return null
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

    private fun convertFormat(audioUri: Uri) {

        val inputAudioPath = requireContext().getInputPath(audioUri)
        val outputFile = selected.getOutputFilePath()
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

        try {
            FFmpegKit.executeAsync(
                cmd.joinToString(" ")
            ) { session ->
                val returnCode = session.returnCode

                if (ReturnCode.isSuccess(returnCode)) {
                    // FFmpeg command was executed successfully
                    // Retrieve the output text which contains the metadata

                    Log.d("format converter", "conversion Successful")
                    Log.d("format converter", cmd.joinToString(" "))

                    requireContext().scanFiles(outputFile)


                } else {
                    // FFmpeg command execution failed
                    // Handle the error or show a message to the user
                    Log.d("format converter", "conversion Failed")

                }
            }
        } catch (e: Exception) {
            Log.d("format converter", "exception: ${e.toString()}")
        }


    }


}