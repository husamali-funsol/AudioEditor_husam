package com.example.audioeditor.ui.fragments.features.recorder.recorder

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.databinding.DiscardDialogBinding
import com.example.audioeditor.databinding.FragmentRecorderBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.utils.deleteFile
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.getCurrentTimestampString
import com.example.audioeditor.utils.getStorageDir
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.setOnOneClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


const val REQUEST_CODE = 200

class RecorderFragment : Fragment(), Timer.OnTimerTickListener {

    private val binding by lazy {
        FragmentRecorderBinding.inflate(layoutInflater)
    }

    private lateinit var recorder: MediaRecorder

    private var permissionsGranted = false

    private var isRecording = false
    private var isPaused = false

    private var file: File? = null

    private val timer by lazy { Timer(this) }


    private var permissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO ,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var filename = ""

    private lateinit var amplitudes: ArrayList<Float>

    private var savingAlertDialog: AlertDialog? = null
    private val savingDialogBinding by lazy {
        SavingDialogBinding.inflate(layoutInflater)
    }
    private var savingDialogView: ConstraintLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getPermissions()

        binding.btnRecord.setOnOneClickListener {
            when{
                isRecording -> stopRecording()
                else -> {
                    startRecording()
                }
            }
        }

        binding.btnPlayPause.setOnOneClickListener {
            when{
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
            }
        }

        binding.btnCancel.setOnOneClickListener {
            pauseRecording()
            showDiscardDialog()
        }



    }

    //***************************************** Start Recording ***********************************************
    private fun startRecording(){
        if (!permissionsGranted) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_CODE)
            return
        }

        // Implement start recording
        recorder = MediaRecorder()

        binding.materialCardView.visibility = View.VISIBLE
        binding.tvTapToRecord.visibility = View.GONE
        binding.ivArrows.visibility = View.GONE

        val outputDir = getStorageDir()
        val recorderFolder = File(outputDir, "FunsolAudioRecorder")
        if (!recorderFolder.exists()) {
            recorderFolder.mkdir()
        }
        filename = "recorded_audio_${getCurrentTimestampString()}"
        val filePath = File(recorderFolder, filename)


        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            //we can save the file in wav format by changing the extension of above file
            //change the file extension to raw and then
            //write the raw audio file to wav audio file.
            setOutputFile("${filePath.absolutePath}.mp3")

            file  = File("${filePath.absolutePath}.mp3")

            try {
                prepare()
            }
            catch (_: IOException){}

            start()

        }

        binding.btnRecord.setImageResource(R.drawable.stop_button)

        isRecording = true
        isPaused = false

        val params = binding.tvRecordingTime.layoutParams as ConstraintLayout.LayoutParams
        params.verticalBias = 0.1f
        binding.tvRecordingTime.layoutParams = params

        timer.start()

        binding.btnCancel.visibility = View.VISIBLE
        binding.btnPlayPause.visibility = View.VISIBLE

        binding.btnPlayPause.setImageResource(R.drawable.ic_pause_light_blue)
    }

    //***************************************** Stop Recording ***********************************************
    private fun stopRecording(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false

        binding.btnPlayPause.visibility = View.GONE
        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        binding.btnCancel.visibility = View.GONE

        binding.btnRecord.setImageResource(R.drawable.record_button)


        binding.tvRecordingTime.text = "00:00:00"

//        //save the raw audio data to wav format
//
//        val rawData = readRawAudioData("$dirPath$filename.raw")
//        saveAudioToFile(rawData, "$dirPath$filename.wav")

        amplitudes = binding.waveform.clear()
        binding.materialCardView.visibility = View.GONE
        binding.tvRecorderStatus.visibility = View.GONE

        binding.tvTapToRecord.visibility = View.VISIBLE
        binding.ivArrows.visibility = View.VISIBLE

        val params = binding.tvRecordingTime.layoutParams as ConstraintLayout.LayoutParams
        params.verticalBias = 0.5f
        binding.tvRecordingTime.layoutParams = params

        file?.let { context?.refreshMediaStore(it) }

        savingDialog()
        updateSavingDialog()

        context?.refreshMediaStoreForAudioFiles()



    }

    //***************************************** Pause Recording ***********************************************
    private fun pauseRecording() {
        recorder.pause()
        isPaused = true
        binding.btnPlayPause.setImageResource(R.drawable.ic_play_light_blue)
        binding.tvRecorderStatus.visibility = View.VISIBLE
        timer.pause()
    }

    //***************************************** Resume Recording ***********************************************
    private fun resumeRecording() {
        recorder.resume()
        isPaused = false
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause_light_blue)
        binding.tvRecorderStatus.visibility = View.GONE
        timer.start()

    }

    //***************************************** Cancel Recording ***********************************************
    private fun cancelOperations() {

        stopRecording()
        file?.let {
            deleteFile(it)
            context?.refreshMediaStore(it)
        }

    }

    //***************************************** Utility Functions ***********************************************
    private fun getPermissions(){
        context?.let{
            permissionsGranted =
                ActivityCompat.checkSelfPermission(it, permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            it,
                            permissions[1]
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            it,
                            permissions[2]
                        ) == PackageManager.PERMISSION_GRANTED

            if (!permissionsGranted) {
                ActivityCompat.requestPermissions(requireActivity(), permissions, REQUEST_CODE)
            }
        }
    }

    //***************************************** Utility Functions ***********************************************
    private fun showDiscardDialog(){

        var discardAlertDialog: AlertDialog? = null
        val discardDialogBinding by lazy {
            DiscardDialogBinding.inflate(layoutInflater)
        }

        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = discardDialogBinding.root
        alertDialogBuilder?.setView(dialogView)
        discardAlertDialog = alertDialogBuilder?.create()

        val quitDialogView = dialogView


        discardDialogBinding.tvCancel.setOnClickListener {
            context?.performHapticFeedback()
            dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardDialogBinding.tvDiscard.setOnClickListener {
            context?.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController().apply {
                if (currentDestination?.id == R.id.mainRecorderFragment) {
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            dismissDialog(discardAlertDialog, quitDialogView)

            cancelOperations()

        }

        discardAlertDialog?.setOnDismissListener {
            dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardAlertDialog!!.show()
    }

    private fun savingDialog(progress: Int = 50) {


        // Update your progress UI or dialog here
        Log.d("AudioEditor", "Progress: $progress%")

        savingDialogBinding.progressBar.progress = 0
        savingDialogBinding.tvSaving.text = "Saving...(0%)"

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

    private fun updateSavingDialog(){
        val totalProgressSteps = 100  // Total progress steps (assuming 100% progress)
        val progressInterval = 20L    // Update progress every 30 milliseconds
        val progressStep = totalProgressSteps / (2000L / progressInterval)  // Calculate the progress step per interval

        // Start a coroutine to gradually update the progress
        GlobalScope.launch(Dispatchers.Main) {
            for (progress in 1..totalProgressSteps) {
                // Update the progress in the dialog
                savingDialogBinding.progressBar.progress = progress
                savingDialogBinding.tvSaving.text = "Saving...($progress%)"
                // Delay for the specified interval before updating again
                delay(progressInterval)
            }

            // Dismiss the saving dialog after the simulated saving is complete
            context?.let {
                dismissDialog(savingAlertDialog, savingDialogView)
            }
        }
    }



    //***************************************** Override Functions ***********************************************
    override fun onTimerTick(duration: String) {
        binding.tvRecordingTime.text = duration
        binding.waveform.visibility = View.VISIBLE
        binding.waveform.addAmplitude(recorder.maxAmplitude.toFloat())
    }

    override fun onDestroy() {
        super.onDestroy()

        if(::recorder.isInitialized && (isRecording || isPaused)){
            cancelOperations()
        }
    }

    override fun onPause() {
        super.onPause()

        if(::recorder.isInitialized && isRecording){
            pauseRecording()
        }
    }

}