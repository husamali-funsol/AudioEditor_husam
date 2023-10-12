package com.example.audioeditor.ui.fragments.features.mergeaudios

import android.app.Activity
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.adapters.AudioItemAdapter
import com.example.audioeditor.databinding.FragmentMergeAudiosBinding
import com.example.audioeditor.databinding.SavingDialogBinding
import com.example.audioeditor.interfaces.CommandExecutionCallback
import com.example.audioeditor.models.AudioItemModel
import com.example.audioeditor.utils.calculateProgress
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.executeCommand
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.getInputPath
import com.example.audioeditor.utils.getOutputFile
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.setOnOneClickListener
import com.example.audioeditor.utils.showSmallLengthToast
import com.masoudss.lib.SeekBarOnProgressChanged
import com.masoudss.lib.WaveformSeekBar
import java.io.File
import java.util.Collections


class MergeAudiosFragment : Fragment(), CommandExecutionCallback {

    private val binding by lazy {
        FragmentMergeAudiosBinding.inflate(layoutInflater)
    }

    private var audiosList: MutableList<AudioItemModel> = ArrayList()
    private var inputPathsList = mutableListOf<String>()

    private val selectedUris = mutableListOf<Uri>()

    private lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var outputFile: File? = null

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


        with(binding){
            btnUpload.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(intent, 100)
            }


            btnPlayPause.setOnOneClickListener {
                context?.performHapticFeedback()
                if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    btnPlayPause.setImageResource(R.drawable.pause_button)
                    updateSeekBar()

                } else if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                    pauseMediaPlayer()
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
                        tvCurrentDurationWindow.text = selectedPositionMillis.toInt().formatDuration()
                        tvCurrentDuration.text = selectedPositionMillis.toInt().formatDuration()
                        mediaPlayer.seekTo(selectedPositionMillis.toInt())
                    }
                }
            }

            ivMinusLeft.setOnClickListener {
                if (::mediaPlayer.isInitialized) {

                    var currentDuration = mediaPlayer.currentPosition.toFloat() / 1000f
                    val totalDuration  = mediaPlayer.duration.toFloat() / 1000f
                    if(currentDuration >= 1){
                        currentDuration-=1
                        tvCurrentDurationWindow.text = (currentDuration*1000).toInt().formatDuration()
                        tvCurrentDuration.text = (currentDuration*1000).toInt().formatDuration()
                        waveform.progress = ((currentDuration/totalDuration) * 100f)
                        mediaPlayer.seekTo((currentDuration*1000).toInt())
                    }

                }
            }


            ivAddLeft.setOnClickListener {
                if (::mediaPlayer.isInitialized) {

                    var currentDuration = mediaPlayer.currentPosition.toFloat() / 1000f
                    val totalDuration  = mediaPlayer.duration.toFloat() / 1000f
                    if(currentDuration <= totalDuration-1){
                        currentDuration+=1
                        tvCurrentDurationWindow.text = (currentDuration*1000).toInt().formatDuration()
                        tvCurrentDuration.text = (currentDuration*1000).toInt().formatDuration()
                        waveform.progress = ((currentDuration/totalDuration) * 100f)
                        mediaPlayer.seekTo((currentDuration*1000).toInt())
                    }

                }
            }



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



    private fun updateSeekBar() {
        updateSeekBarHandler.postDelayed(updateSeekBarRunnable, 100)
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
                val progress = mediaPlayer.calculateProgress()
                binding.waveform.progress = progress
                binding.tvCurrentDuration.text = mediaPlayer.currentPosition.formatDuration()
                binding.tvCurrentDurationWindow.text = mediaPlayer.currentPosition.formatDuration()

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







    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->

                // Check if multiple files were selected
                if (intent.clipData != null) {
                    val clipData = intent.clipData
//                    for (i in 0 until clipData?.itemCount!!) {
                    Log.d("adsfsdf", "onActivityResult: ${clipData?.itemCount!!}")
                    val itemCount = clipData.itemCount.takeIf { it <= 3 } ?: 3
                    if(clipData.itemCount > 3){
                        context?.showSmallLengthToast("3 audios can be merged at a time")
                    }

                    if (audiosList.size<3){
                        for (i in 0 until itemCount) {
                            val uri = clipData.getItemAt(i)?.uri
                            if (uri != null) {
                                selectedUris.add(uri)
                            }
                            val newItem = AudioItemModel(
                                "${
                                    uri?.let {
                                        context?.getFileNameFromUri(
                                            it
                                        )
                                    }
                                }"
                            )
                            audiosList.add(newItem)
                        }
                    }
                    else{
                        context?.showSmallLengthToast("You have already uploaded 3 audios")
                    }
                } else if (intent.data != null) {
                    // Only one file was selected
                    if (audiosList.size<3){
                        val uri = intent.data!!
                        selectedUris.add(uri)
                        val newItem = AudioItemModel("${context?.getFileNameFromUri(uri)}")
                        audiosList.add(newItem)
                    }
                    else{
                        context?.showSmallLengthToast("You have already uploaded 3 audios")
                    }
                }

                //recycler view
                binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                val adapter = AudioItemAdapter(audiosList)
                binding.recyclerView.adapter = adapter

                val itemTouchHelper =
                    ItemTouchHelper(object : ItemTouchHelper.Callback() {
                        override fun getMovementFlags(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder
                        ): Int {
                            return makeMovementFlags(
                                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                                ItemTouchHelper.START
                            )
                        }

                        override fun onMove(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder
                        ): Boolean {


                            var sourceIndex = viewHolder.adapterPosition
                            var targetIndex = target.adapterPosition

                            Collections.swap(audiosList, sourceIndex, targetIndex)
                            Collections.swap(selectedUris, sourceIndex, targetIndex)

                            adapter.notifyItemMoved(sourceIndex, targetIndex)
                            binding.recyclerView.adapter = adapter

                            return false
                        }

                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            when (direction) {
                                ItemTouchHelper.START -> {
                                    audiosList.removeAt(viewHolder.adapterPosition)
                                    selectedUris.removeAt(viewHolder.adapterPosition)
                                    adapter.notifyItemRemoved(viewHolder.adapterPosition)
                                    binding.recyclerView.adapter = adapter

                                }

                            }

                        }
                    })

                itemTouchHelper.attachToRecyclerView(binding.recyclerView)


                if(selectedUris.size>1){
                    pauseMediaPlayer()
                    savingDialog()
                    mergeAudio(selectedUris)
                }
                else{
                    val path = context?.getInputPath(selectedUris[0])
                    createMediaPlayer(null, path)
                }
            }
        }
    }



    private fun mergeAudio(urisList: MutableList<Uri>) {

            outputFile = "filename".getOutputFile("mp3")
            val endDurationsList = getEndDurations(urisList)

            val inputFiles = getInputFiles(urisList)
            val filterComplex = getFilterComplex(urisList, endDurationsList)

            val cmd = arrayOf(
                "-y",
                inputFiles,
                "-filter_complex",
                filterComplex,
                "-map", "[out]",
                outputFile!!.path
            )

            Log.e("AudioFile", "command: ${cmd.joinToString(" ")}")


        cmd.executeCommand(this)

    }




    private fun getEndDurations(urisList: MutableList<Uri>): MutableList<Int> {

        val endDurationsList = mutableListOf<Int>()

        for (i in urisList) {
            val file = File(i.path!!)
            val x = getAudioDurationFromUri(i)
            Log.d(TAG, "getEndDurations: $i $x")
            if (x != null) {
                endDurationsList.add(x.toInt())
            }
        }
        return endDurationsList
    }


    private fun getAudioDurationFromUri(uri: Uri): Int? {

        val mediaMetadataRetriever = MediaMetadataRetriever()

        try {
            // Set the data source for the media metadata retriever
            mediaMetadataRetriever.setDataSource(requireContext(), uri)

            // Get the duration of the audio in milliseconds
            val durationInMillisString =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            // Convert the duration to milliseconds and return
            return (durationInMillisString?.toInt())?.div(1000)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaMetadataRetriever.release()
        }

        return null
    }

    private fun getInputFiles(urisList: MutableList<Uri>): String {

        val inputFiles = StringBuilder()

        urisList.forEachIndexed { index, _ ->
            inputFiles.append("-i ${context?.getInputPath(urisList[index])} ")
        }

        return inputFiles.toString()

    }

    private fun pauseMediaPlayer() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
            binding.btnPlayPause.setImageResource(R.drawable.play_button)
        }
    }

    private fun getFilterComplex(
        urisList: MutableList<Uri>,
        endDurationsList: MutableList<Int>
    ): String {

        var noOfAudios = 0

        val filterComplex = StringBuilder()

        urisList.forEachIndexed { index, uri ->

            filterComplex.append("[$index:a]atrim=start=0:end=${endDurationsList[index]},asetpts=PTS-STARTPTS[a${index + 1}];")
            ++noOfAudios
        }

        val names = StringBuilder()
        urisList.forEachIndexed { index, uri ->
            names.append("[a${index + 1}]")
        }

        filterComplex.append("${names.toString()}concat=n=${noOfAudios}:v=0:a=1[out]")
        return filterComplex.toString()
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
                if (path != null) {
                    setDataSource(it, Uri.parse(path))
                }


            }
            prepareAsync()

            setOnPreparedListener { mp ->
                binding.waveform.visibility = View.VISIBLE
//                binding.cropWindowTrim.visibility = View.VISIBLE
//                binding.cropWindowTrim.setCurrentValues(0f, 1f)
//                cropLeft = 0f
//                cropRight = 1f
                if (uri != null) {
                    binding.waveform.setSampleFrom(uri)
                }

                else if (path != null) {
                    binding.waveform.setSampleFrom(Uri.fromFile(File(path)))
                }

                binding.waveform.waveWidth = 4F
                binding.waveform.maxProgress = 100F

                val durationMillis = mediaPlayer.duration
                binding.tvCurrentDuration.text = 0.formatDuration()
                Log.d(TAG, "createMediaPlayer: $durationMillis")
                binding.tvEndDuration.text = mediaPlayer.duration.formatDuration()


                binding.tvCurrentDurationWindow.text = 0.formatDuration()
                savingDialogBinding.progressBar.progress = 100
                savingDialogBinding.tvSaving.text = "Processing Done!"
                dialogDismiss()

                mp.setOnCompletionListener {
                    binding.waveform.progress = 0F
                    mediaPlayer.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.play_button)
                    binding.tvCurrentDuration.text = 0.formatDuration()

                }

            }
        }
    }

    private fun dialogDismiss() {
        Handler().postDelayed({
            dismissDialog(savingAlertDialog, savingDialogView)
        }, 1000)
    }
    override fun onCommandExecutionSuccess() {
        createMediaPlayer(null, outputFile?.path)

    }

    override fun onCommandExecutionFailure(errorMessage: String) {
        TODO("Not yet implemented")
    }


}