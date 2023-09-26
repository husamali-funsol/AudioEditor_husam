package com.example.audioeditor.ui.fragments.features.trimaudio

import AudioFileContract
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentTrimAudioBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.lib.rangeview.RangeView
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.convertMillisToMinutes
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.getExtensionFromUri
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.setOnOneClickListener
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar


class TrimAudio : Fragment() {

    private val binding by lazy {
        FragmentTrimAudioBinding.inflate(layoutInflater)
    }

    private var audioUri: Uri? = null

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
    private val quitDialogBinding by lazy{
        QuitDialogBinding.inflate(layoutInflater)
    }
    private var quitDialogView: ConstraintLayout? = null

    private var outputPath = ""
    private var cropLeft: Float = 0F
    private var cropRight: Float = 1F

    private var trimIn = false
    private var trimOut = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUpload.setOnOneClickListener {

        }

        binding.btnUpload.setOnClickListener {
            audioFileLauncher.launch("audio/*")
            context?.performHapticFeedback()
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


                }

                override fun rightTogglePositionChanged(xCoordinate: Float, value: Float) {
                    cropRight = value
                    val audioDurationMillis = mediaPlayer.duration
                    val progress = (value * 100) - 3
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    binding.tvCropWindowRight.text = selectedPositionMillis.toInt().formatDuration()
                    mediaPlayer.seekTo(selectedPositionMillis.toInt())
                }
            }

        binding.cardTrimIn.setOnOneClickListener {
            if(!trimIn) {
                binding.cardTrimOut.setBackgroundResource(R.drawable.button_bg_unselected)
                binding.cardTrimIn.setBackgroundResource(R.drawable.button_bg_selected)
                binding.ivTrimInIcon.setImageResource(R.drawable.ic_trim_in_selected)
                binding.ivTrimOutIcon.setImageResource(R.drawable.ic_trim_out_unselected)
                binding.tvTrimIn.setTextColor(resources.getColor(R.color.white))
                binding.tvTrimOut.setTextColor(resources.getColor(R.color.textColorlightGrey))
                binding.cropWindowTrim.visibility = View.VISIBLE
                trimIn = true
                trimOut = false
            }
            else{
                binding.cardTrimIn.setBackgroundResource(R.drawable.button_bg_unselected)
                binding.ivTrimInIcon.setImageResource(R.drawable.ic_trim_in_unselected)
                binding.tvTrimIn.setTextColor(resources.getColor(R.color.textColorlightGrey))
                binding.cropWindowTrim.visibility = View.GONE
                trimOut = false
                trimIn = false
            }
        }

        binding.cardTrimOut.setOnOneClickListener {
            if(!trimOut)
            {
                binding.cardTrimIn.setBackgroundResource(R.drawable.button_bg_unselected)
                binding.cardTrimOut.setBackgroundResource(R.drawable.button_bg_selected)
                binding.ivTrimInIcon.setImageResource(R.drawable.ic_trim_in_unselected)
                binding.ivTrimOutIcon.setImageResource(R.drawable.ic_trim_out_selected)
                binding.tvTrimOut.setTextColor(resources.getColor(R.color.white))
                binding.tvTrimIn.setTextColor(resources.getColor(R.color.textColorlightGrey))
                binding.cropWindowTrim.visibility = View.VISIBLE
                trimOut = true
                trimIn = false
//                binding.cropWindowTrim.trimOutRangeView()
            }
            else{
                binding.cardTrimOut.setBackgroundResource(R.drawable.button_bg_unselected)
                binding.ivTrimOutIcon.setImageResource(R.drawable.ic_trim_out_unselected)
                binding.tvTrimOut.setTextColor(resources.getColor(R.color.textColorlightGrey))
                binding.cropWindowTrim.visibility = View.GONE
                trimIn = false
                trimOut = false
            }
        }

    }

    private val audioFileLauncher = registerForActivityResult(AudioFileContract()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri

            createMediaPlayer(audioUri!!)
//            setMetadata(audioUri!!)
            extension = context?.getExtensionFromUri(uri)
            binding.tvMusicTitle.text = context?.getFileNameFromUri(uri)
        }
    }

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
                binding.waveform.visibility = View.VISIBLE
                binding.waveform.setSampleFrom(uri)
                binding.waveform.waveWidth = 4F
                binding.waveform.maxProgress = 100F

                val durationMillis = mp.duration
                binding.tvCurrentDuration.text = mp.currentPosition.formatDuration()
                binding.tvEndDuration.text = durationMillis.formatDuration()

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

}