package com.example.audioeditor.ui.fragments.library.audioplayer

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.audioeditor.R
import com.example.audioeditor.databinding.BottomSheetDetailsBinding
import com.example.audioeditor.databinding.BottomSheetLibraryBinding
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.FragmentLibraryAudioPlayerBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.utils.convertMillisToMinutes
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles
import com.example.audioeditor.utils.setOnOneClickListener
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LibraryAudioPlayerFragment : Fragment() {

    private val binding by lazy {
        FragmentLibraryAudioPlayerBinding.inflate(layoutInflater)
    }

    private val libraryBottomSheetDialogBinding by lazy {
        BottomSheetLibraryBinding.inflate(layoutInflater)
    }
    private val detailsBottomSheetDialogBinding by lazy {
        BottomSheetDetailsBinding.inflate(layoutInflater)
    }
    private val renameDialogBinding by lazy {
        RenameDialogBinding.inflate(layoutInflater)
    }
    private val deleteDialogBinding by lazy {
        DeleteDialogBinding.inflate(layoutInflater)
    }

    lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private var audioUri: Uri? = null
    private var libItem: LibraryItemModel? = null

    private var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val libItem = arguments?.getParcelable<LibraryItemViewModel>("AUDIO_ITEM")

        val mList = arguments?.getParcelableArray("AUDIO_ITEMS") as? Array<LibraryItemModel>
        var position = arguments?.getInt("AUDIO_POSITION", -1)

        libItem = mList?.get(position!!)

        audioUri = Uri.parse("")
//        Log.d("library player debug", str.toString())

        setAudioFileData()
//
        audioUri?.let{ createMediaPlayer(it) }

        binding.ibBack.setOnOneClickListener {
            findNavController().apply {
                popBackStack()
            }
        }

        binding.ivSkipNextLP.setOnOneClickListener {
            skipNext(position, mList)
        }

        binding.ivSkipPrevLP.setOnOneClickListener {
            skipPrevious(position, mList)
        }

        binding.ivPlayerLP.setOnClickListener {
            if (!(mediaPlayer.isPlaying)) {
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.start()
                    binding.ivPlayerLP.setImageResource(R.drawable.pause_button)
                    updateSeekBar()
                }
            } else {
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.pause()
                    binding.ivPlayerLP.setImageResource(R.drawable.play_button)
                    updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
                }
            }
        }

        binding.sbLP.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    val audioDurationMillis = mediaPlayer.duration
                    val selectedPositionMillis = (progress * audioDurationMillis) / 100
                    mediaPlayer.seekTo(selectedPositionMillis)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.ibMoreLP.setOnOneClickListener {

            openAudioBottomSheet(position)
        }
    }



    //*****************************************  Media Player Functions  ***********************************************

    private fun createMediaPlayer(filepath: Uri) {
        val uri = Uri.fromFile(File(filepath.toString()))

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer().apply {
            context?.let{ setDataSource(it, uri) }
            prepareAsync()

            setOnPreparedListener { mp ->
                val durationMillis = mp.duration
                binding.sbLP.max = 100 // We set the maximum progress to 100 to represent 100%
                binding.sbLP.progress = 0 // Initially set progress to 0

                binding.tvStartLp.text = "00:00"
                binding.tvEndLP.text = durationMillis.toLong().convertMillisToMinutes()

                mp.setOnCompletionListener {
                    binding.sbLP.progress = 0
                    mediaPlayer.start()
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
                val audioDurationMillis = mediaPlayer.duration
                val currentPositionMillis = mediaPlayer.currentPosition
                val progress = (currentPositionMillis.toFloat() / audioDurationMillis) * 100
                binding.sbLP.progress = progress.toInt()
                binding.tvStartLp.text = currentPositionMillis.toLong().convertMillisToMinutes()
                // Update the SeekBar position every 100 milliseconds
                updateSeekBarHandler.postDelayed(this, 100)
            }
        }
    }

    //*****************************************  Dialogs  ***********************************************

    private fun showRenameDialog(
        position: Int?,
        bottomSheet: BottomSheetDialog
    ) {
        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }
        val parent = renameDialogBinding.root.parent as? ViewGroup
        parent?.removeView(renameDialogBinding.root)
        val dialogView = renameDialogBinding.root
        alertDialogBuilder?.setView(dialogView)

        if (libItem != null) {
            // Set the initial text in your EditText (if needed)
            renameDialogBinding.etRenameRD.setText(libItem!!.title)
            renameDialogBinding.etRenameRD.setSelection(renameDialogBinding.etRenameRD.length())//placing cursor at the end of the text
            renameDialogBinding.etRenameRD.setSelectAllOnFocus(true)
            renameDialogBinding.etRenameRD.highlightColor = resources.getColor(R.color.thirtyPrecentAppBlue)

        }

        renameDialogBinding.tvConfirmRD.setOnClickListener {
            // Handle the positive button click event here
            // You can retrieve the text entered in the EditText like this:
            val enteredText = renameDialogBinding.etRenameRD.text.toString()

            // Implement your logic here (e.g., renameFile(enteredText))
            val ext = libItem!!.extension
            renameFile(enteredText, ext!!, libItem!!, position!!)

            alertDialog?.dismiss()
        }

        renameDialogBinding.tvCancelRD.setOnClickListener {
            // Handle the negative button click event here
            // This is where you can cancel the dialog if needed
            alertDialog?.dismiss()

        }

        alertDialog = alertDialogBuilder?.create()
        bottomSheet.dismiss()
        alertDialog!!.show()
    }

    private fun showDeleteDialog(bottomSheet: BottomSheetDialog) {
        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }
        val parent = deleteDialogBinding.root.parent as? ViewGroup
        parent?.removeView(deleteDialogBinding.root)
        val dialogView = deleteDialogBinding.root
        alertDialogBuilder?.setView(dialogView)


        deleteDialogBinding.tvDeleteBtnDD.setOnClickListener {
            // Handle the positive button click event here
            val filePath =
                libItem?.path
            val originalFile = File(filePath!!)

            if (originalFile.exists()) {
                originalFile.delete()
                context?.scanFiles(originalFile)

            }

            context?.refreshMediaStoreForAudioFiles()

            alertDialog?.dismiss()
            findNavController().popBackStack()
        }

        deleteDialogBinding.tvCancelDD.setOnClickListener {
            alertDialog?.dismiss()
        }


        alertDialog = alertDialogBuilder?.create()
        bottomSheet.dismiss()
        alertDialog!!.show()
    }


    //*****************************************  Bottom Sheet  ***********************************************

    private fun openDetailsBottomSheet(bottomSheet: BottomSheetDialog) {
        val detailsBottomSheet = BottomSheetDialog(requireContext())
        val parent = detailsBottomSheetDialogBinding.root.parent as? ViewGroup
        parent?.removeView(detailsBottomSheetDialogBinding.root)
        detailsBottomSheet.setContentView(detailsBottomSheetDialogBinding.root)

        if (libItem != null) {
            detailsBottomSheetDialogBinding.tvSetFilename.text = libItem!!.title
            detailsBottomSheetDialogBinding.tvSetTime.text = libItem!!.time
            detailsBottomSheetDialogBinding.tvSetPath.text = libItem!!.path
            detailsBottomSheetDialogBinding.tvSetSize.text = libItem!!.size
        }
        bottomSheet.dismiss()

        detailsBottomSheetDialogBinding.tvOkDetails.setOnClickListener {
            detailsBottomSheet.dismiss()
        }

        detailsBottomSheet.show()
    }

    private fun openAudioBottomSheet(position: Int?) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = libraryBottomSheetDialogBinding.root.parent as? ViewGroup
        parent?.removeView(libraryBottomSheetDialogBinding.root)
        bottomSheet.setContentView(libraryBottomSheetDialogBinding.root)

        if (libItem != null) {
            libraryBottomSheetDialogBinding.tvTitleLibSheet.text = libItem!!.title
            libraryBottomSheetDialogBinding.tvMetaDataLibSheet.text = libItem!!.metadata
        }

        libraryBottomSheetDialogBinding.viewRename.setOnClickListener {
            //                renameDialogBinding = RenameDialogBinding.inflate(layoutInflater)
            showRenameDialog(position, bottomSheet)
        }

        libraryBottomSheetDialogBinding.viewDetail.setOnClickListener {
            openDetailsBottomSheet(bottomSheet)
        }

        libraryBottomSheetDialogBinding.viewShare.setOnClickListener {
            openShareIntent(bottomSheet)
        }

        libraryBottomSheetDialogBinding.viewDelete.setOnClickListener {
            //                deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
            showDeleteDialog(bottomSheet)

        }
        bottomSheet.show()
    }

    private fun openShareIntent(bottomSheet: BottomSheetDialog) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "audio/*"

        shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri)

        bottomSheet.dismiss()

        startActivity(Intent.createChooser(shareIntent, "Share Audio"))
    }


    //*****************************************  Utility Functions  ***********************************************

    private fun skipPrevious(
        position: Int?,
        mList: Array<LibraryItemModel>?
    ) {
        var position1 = position
        position1 = position1!! - 1

        if (position1 != null && position1!! >= 0 && position1!! < (mList?.size ?: 0)) {
            libItem = mList?.get(position1!!)
        }
        if (libItem != null) {
            audioUri = libItem!!.uri!!
            binding.tvTitleLP.text = libItem!!.title
            binding.tvMetadataLP.text = libItem!!.metadata
            // Use Glide to load and set the image
            context?.let {
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.music_thumbnail) // Placeholder image
                    .error(R.drawable.music_thumbnail) // Error image
                    .into(binding.imageView) // Set the ImageView you want to load the image into
                // Use Glide to load and set the image
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.placeholder_image) // Placeholder image
                    .error(R.drawable.placeholder_image) // Error image
                    .into(binding.ivAvatarLibItem) // Set the ImageView you want to load the image into
            }
            binding.ivPlayerLP.setImageResource(R.drawable.play_button)


            Log.d("get path", libItem!!.path.toString())
        }
        //
        audioUri?.let { createMediaPlayer(it) }
    }

    private fun skipNext(
        position: Int?,
        mList: Array<LibraryItemModel>?
    ) {
        var position1 = position
        position1 = position1!! + 1

        if (position1 != null && position1!! >= 0 && position1!! < (mList?.size ?: 0)) {
            libItem = mList?.get(position1!!)
        }
        if (libItem != null) {
            audioUri = libItem!!.uri!!
            binding.tvTitleLP.text = libItem!!.title
            binding.tvMetadataLP.text = libItem!!.metadata
            // Use Glide to load and set the image
            context?.let {
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.music_thumbnail) // Placeholder image
                    .error(R.drawable.music_thumbnail) // Error image
                    .into(binding.imageView) // Set the ImageView you want to load the image into
                // Use Glide to load and set the image
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.placeholder_image) // Placeholder image
                    .error(R.drawable.placeholder_image) // Error image
                    .into(binding.ivAvatarLibItem) // Set the ImageView you want to load the image into
            }
            binding.ivPlayerLP.setImageResource(R.drawable.play_button)


            Log.d("get path", libItem!!.path.toString())
        }
        //
        audioUri?.let { createMediaPlayer(it) }
    }

    private fun setAudioFileData() {
        if (libItem != null) {
            audioUri = libItem!!.uri!!
            binding.tvTitleLP.text = libItem!!.title
            binding.tvMetadataLP.text = libItem!!.metadata
            // Use Glide to load and set the image
            context?.let{
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.music_thumbnail) // Placeholder image
                    .error(R.drawable.music_thumbnail) // Error image
                    .into(binding.imageView) // Set the ImageView you want to load the image into
                // Use Glide to load and set the image
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.placeholder_image) // Placeholder image
                    .error(R.drawable.placeholder_image) // Error image
                    .into(binding.ivAvatarLibItem) // Set the ImageView you want to load the image into
            }

            Log.d("get path", libItem!!.path.toString())
        }
    }

    private fun renameFile(newName: String, ext: String, libItem: LibraryItemModel, position: Int) {
        val filePath =
            libItem.path
        val newFileName = "$newName.$ext" // Provide the new file name
        val originalFile = File(filePath!!)
        // Create a File object for the new file with the desired name
        val directoryPath = originalFile.parentFile // Get the directory path
        val newFile = File(directoryPath, newFileName)

        // Rename the file
        if (originalFile.exists()) {
            if (originalFile.renameTo(newFile)) {

                val newPath =
                    newFile.path
                val updatedFile = File(newPath!!)

                val currentTimeMillis = System.currentTimeMillis()
                newFile.setLastModified(currentTimeMillis)
                originalFile.setLastModified(currentTimeMillis)
                libItem.time =  SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(
                    Date(currentTimeMillis)
                )
                // Refresh the MediaStore to reflect the changes
                context?.refreshMediaStore(updatedFile)
                context?.showSmallLengthToast("Renaming Successful")
//                adapter.itemUpdated(position, viewModel.getSingleFile(position))

            } else {
                // Failed to rename the file
                // Handle the error accordingly
                context?.showSmallLengthToast("Renaming Failed")
            }
        } else {
            // The original file does not exist
            context?.showSmallLengthToast("Original File does not exist")
        }

        val newPath =
            newFile.path

        libItem.path = newPath

        val updatedFile = File(newPath)
        context?.refreshMediaStore(updatedFile)

        context?.refreshMediaStoreForAudioFiles()
//        getList()

        val fileNameWithExtension = updatedFile.name // This gives you "file.txt"
        binding.tvTitleLP.text = fileNameWithExtension.substringBeforeLast(".")
        libItem.title = fileNameWithExtension.substringBeforeLast(".")


    }


    //*****************************************  Override Functions  ***********************************************

    override fun onDestroy() {
        super.onDestroy()

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }

    override fun onPause() {
        super.onPause()

        if(::mediaPlayer.isInitialized && mediaPlayer.isPlaying){
            mediaPlayer.pause()
            binding.ivPlayerLP.setImageResource(R.drawable.play_button)
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }



}