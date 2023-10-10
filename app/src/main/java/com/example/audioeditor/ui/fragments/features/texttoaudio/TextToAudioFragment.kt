package com.example.audioeditor.ui.fragments.features.texttoaudio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.FragmentTextToAudioBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getTemporaryFileInPrivateDirectory
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.replaceSpaceWithUnderscore
import com.example.audioeditor.utils.setOnOneClickListener
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.fixedRateTimer


class TextToAudioFragment : Fragment(), TextToSpeech.OnInitListener {

    private val binding by lazy {
        FragmentTextToAudioBinding.inflate(layoutInflater)
    }

    private var savingAlertDialog: AlertDialog? = null
    private val savingDialogBinding by lazy {
        SavingDialogBinding.inflate(layoutInflater)
    }
    private var savingDialogView: ConstraintLayout? = null

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()
    private val createPlayerHandler = Handler()
    var count = 0

    private lateinit var textToSpeech: TextToSpeech

    private var outputFile: File? = null
    private var previousOutputFile: File? = null

    private var selectedVoice = "Female"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textToSpeech = TextToSpeech(requireContext(), this)


        with(binding){

            waveform.isClickable = false
            btnPlayPause.isClickable = false
            tvDuration.isClickable = false

            tvDuration.visibility = View.GONE
            waveform.visibility = View.GONE
            btnPlayPause.visibility = View.GONE
            loader.visibility = View.GONE


            wholeView.setOnTouchListener { v, event ->
                getFocus(event, v)
            }

            btnBack.setOnOneClickListener {
                context?.performHapticFeedback()
                clearEditTextFocus(etText)
                showQuitDialog()
            }

            btnSave.setOnOneClickListener {
                clearEditTextFocus(etText)
                if (::mediaPlayer.isInitialized) {
                    showRenameDialog()
                }


            }

            etText.addTextChangedListener(
                object : TextWatcher {

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

                    private var timer: Timer = Timer()
                    private val DELAY: Long = 1000 // Milliseconds
                    override fun afterTextChanged(s: Editable) {
                        setLoadingViews()

                        timer.cancel()
                        timer = Timer()
                        timer.schedule(
                            object : TimerTask() {
                                override fun run() {
                                    generateAudio()
                                }
                            },
                            DELAY
                        )
                    }
                }
            )

            btnPlayPause.setOnClickListener {
                clearEditTextFocus(etText)

//                val textToSpeak = binding.etText.text.toString()
//                speak(textToSpeak)

                context?.performHapticFeedback()
                if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    btnPlayPause.setImageResource(R.drawable.ic_pause_blue)
                    updateSeekBar()

                } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    btnPlayPause.setImageResource(R.drawable.ic_play_blue)
                    updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
                }
            }


            tvFemale.setOnOneClickListener {
                clearEditTextFocus(etText)

                onTextViewClick(tvFemale)
            }

            tvMale.setOnOneClickListener {
                clearEditTextFocus(etText)

                onTextViewClick(tvMale)
            }

            tvRobot.setOnOneClickListener {
                clearEditTextFocus(etText)

                onTextViewClick(tvRobot)
            }

            tvGirl.setOnOneClickListener {
                clearEditTextFocus(etText)

                onTextViewClick(tvGirl)
            }
            tvBoy.setOnOneClickListener {
                clearEditTextFocus(etText)

                onTextViewClick(tvBoy)
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


    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(
            binding.tvFemale,
            binding.tvMale,
            binding.tvRobot,
            binding.tvGirl,
            binding.tvBoy,
        )
        for (textView in allTextViews) {

            context?.let {
                textView.setBackgroundResource(R.drawable.button_bg_grey_rounded)
                textView.setTextColor(
                    ContextCompat.getColor(
                        it,
                        R.color.textColorBlack
                    )
                )
            }
        }

        selectedVoice = clickedTextView.text.toString()
        Log.d(TAG, "onTextViewClick: $selectedVoice")
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


    private fun FragmentTextToAudioBinding.setLoadingViews() {
        CoroutineScope(Dispatchers.Main).launch {
            loader.visibility = View.VISIBLE
            tvDuration.visibility = View.GONE
            waveform.visibility = View.GONE
            btnPlayPause.visibility = View.GONE

            waveform.isClickable = false
            btnPlayPause.isClickable = false
            tvDuration.isClickable = false

        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.ERROR) {

            changeVoice()

            textToSpeech.language = Locale.US
        }
    }

    private fun changeVoice(){
        when(selectedVoice){

            "Female" -> {
                textToSpeech.setPitch(1f)
                textToSpeech.setSpeechRate(0.8f)
            }

            "Male" -> {
                textToSpeech.setPitch(0.8f)
                textToSpeech.setSpeechRate(0.8f)
            }

            "Robot" -> {
                textToSpeech.setPitch(1.2f)
                textToSpeech.setSpeechRate(0.8f)
            }

            "Girl" -> {
                textToSpeech.setPitch(1f)
                textToSpeech.setSpeechRate(1f)
            }

            "Boy" -> {
                textToSpeech.setPitch(0.8f)
                textToSpeech.setSpeechRate(1f)
            }

        }
    }

    private fun generateAudio() {
        changeVoice()

        outputFile?.delete()
        outputFile = context?.getTemporaryFileInPrivateDirectory("mp3")
        val outPath = outputFile?.path
        val text = binding.etText.text.toString()
        val myHashRender: HashMap<String?, String?> = HashMap()
        val wakeUpText = "Are you up yet?"
        myHashRender[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = wakeUpText
        textToSpeech.synthesizeToFile(text, myHashRender, outPath)

        val voices = textToSpeech.voices

        Log.d(TAG, "generateAudio: $voices")
        
        createPlayerHandler.postDelayed(createPlayerRunnable, 100)
        
    }

    private val createPlayerRunnable = object : Runnable {
        override fun run() {

            if (outputFile != null) {
                if(outputFile!!.length() > 0L && outputFile!!.exists()) {

                    Log.d(TAG, "generateAudio: Success")

                        checkFileSize(outputFile!!.path, outputFile!!.length())
//                        createMediaPlayer(null, outputFile!!.path)


                }
                else{
                    Log.d(TAG, "run: else")
                    createPlayerHandler.postDelayed(this, 100)
                }

            }
        }
    }

    fun checkFileSize(filePath: String, initialSize: Long) {
        var previousSize = initialSize
        var unchangedCount = 0

        fixedRateTimer(period = 200) {
            val file = File(filePath)
            val currentSize = file.length()

            if (currentSize == previousSize) {
                unchangedCount++
                if (unchangedCount >= 3) {
                    println("File size remains constant. Assuming it's fully written.")
                    createMediaPlayer(null, filePath)
                    cancel()
                }
            } else {
                unchangedCount = 0
            }

            previousSize = currentSize
        }
    }


    private fun getFocus(event: MotionEvent, v: View): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (binding.etText.isFocused) {
                val outRect = Rect()
                binding.etText.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawX.toInt())) {
                    clearEditTextFocus(v)
                }
            }
        }
        return false
    }

    private fun clearEditTextFocus(v: View) {
        binding.etText.clearFocus()
        val imm =
            v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
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

        val renameDialogBinding by lazy {
            RenameDialogBinding.inflate(layoutInflater)
        }
        var renameAlertDialog: AlertDialog? = null
        var renameDialogView: ConstraintLayout? = null

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

            pauseMediaPlayer()
//            trimAndChangeFormatAudio(name)
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

    private fun pauseMediaPlayer() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            binding.btnPlayPause.setImageResource(R.drawable.ic_play_blue)
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
//                binding.tvCurrentDuration.text = mediaPlayer.currentPosition.formatDuration()

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


    private fun createMediaPlayer(uri: Uri? = null, path: String? = null) {

        try{
            if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer().apply {
            count++
            Log.d(TAG, "createMediaPlayer: $count")

            context?.let {
                if (uri != null) {
                    setDataSource(it, uri)
                }
                else if (path != null) {
                    Log.d(TAG, "createMediaPlayer: path $count")
                    setDataSource(path)
                }
            }

            prepareAsync()

            setOnPreparedListener { mp ->
                Log.d(TAG, "createMediaPlayer: prepared")
                handleOnPrepared(path, mp)
            }
        }
        }
        catch (e: IOException) {
            // Handle IOException here
            e.printStackTrace()
        }
    }

    private fun handleOnPrepared(path: String?, mp: MediaPlayer) {
        with(binding) {
            waveform.setSampleFrom(File(path!!))
            loader.visibility = View.GONE
            tvDuration.visibility = View.VISIBLE
            btnPlayPause.visibility = View.VISIBLE
            waveform.visibility = View.VISIBLE

            waveform.isClickable = true
            btnPlayPause.isClickable = true
            tvDuration.isClickable = true

            waveform.waveWidth = 4F
            waveform.maxProgress = 100F

            val durationMillis = mediaPlayer.duration
            Log.d(TAG, "createMediaPlayer: $durationMillis")
            tvDuration.text = mediaPlayer.duration.formatDuration()

            mp.setOnCompletionListener {
                waveform.progress = 0F
                mediaPlayer.pause()
                btnPlayPause.setImageResource(R.drawable.ic_play_blue)
            }
        }
    }


}