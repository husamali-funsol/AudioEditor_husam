package com.example.audioeditor.ui.fragments.library.videoplayer

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.audioeditor.R
import com.example.audioeditor.databinding.BottomSheetDetailsBinding
import com.example.audioeditor.databinding.BottomSheetLibraryBinding
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.FragmentLibraryVideoPlayerBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles
import com.example.audioeditor.utils.setOnOneClickListener
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File


class LibraryVideoPlayerFragment : Fragment() {

    private val binding by lazy {
        FragmentLibraryVideoPlayerBinding.inflate(layoutInflater)
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

    private var videoUri: Uri? = null
    private var libItem: LibraryItemModel? = null

    private var alertDialog: AlertDialog? = null

    private lateinit var player: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mList = arguments?.getParcelableArray("VIDEO_ITEMS") as? Array<LibraryItemModel>
        var position = arguments?.getInt("VIDEO_POSITION", -1)

        libItem = mList?.get(position!!)

        videoUri = Uri.parse("")

        setVideoFileData()

        videoUri?.let{ createVideoPlayer(it) }

        binding.ibMore.setOnOneClickListener {

            openAudioBottomSheet(position)
        }

    }


    private fun setVideoFileData() {
        if (libItem != null) {
            videoUri = libItem!!.uri!!
            binding.tvMusicTitle.text = libItem!!.title
            binding.tvMetadata.text = libItem!!.metadata
            // Use Glide to load and set the image
            context?.let{
                Glide.with(it)
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.music_thumbnail) // Placeholder image
                    .error(R.drawable.music_thumbnail) // Error image
                    .into(binding.ivAlbumImage) // Set the ImageView you want to load the image into

            }

            Log.d("get path", libItem!!.path.toString())
        }
    }



    private fun createVideoPlayer(videoUri: Uri) {


        player = ExoPlayer.Builder(requireContext()).build()

        // Bind the player to the view.
        binding.playerView.player = player

        // Build the media item.
        val mediaItem = MediaItem.fromUri(videoUri)
        // Set the media item to be played.
        player.setMediaItem(mediaItem)
        // Prepare the player.
        player.prepare()
        // Start the playback.
        player.play()

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
            openShareIntent()
        }

        libraryBottomSheetDialogBinding.viewDelete.setOnClickListener {
            //                deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
            showDeleteDialog(bottomSheet)

        }
        bottomSheet.show()
    }

    private fun openShareIntent() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "audio/*"

        shareIntent.putExtra(Intent.EXTRA_STREAM, videoUri)

        startActivity(Intent.createChooser(shareIntent, "Share Audio"))
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

    //*****************************************  Utility Functions  ***********************************************

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
            context?.showSmallLengthToast("Original File doen not exist")
        }

        val newPath =
            newFile.path

        val updatedFile = File(newPath)
        context?.refreshMediaStore(updatedFile)

        context?.refreshMediaStoreForAudioFiles()
//        getList()

        val fileNameWithExtension = updatedFile.name // This gives you "file.txt"
        binding.tvMusicTitle.text = fileNameWithExtension.substringBeforeLast(".")

    }

    //*****************************************  Override Functions  ***********************************************

    override fun onDestroy() {
        super.onDestroy()

        if (::player.isInitialized) {
            player.release()
        }
    }

    override fun onPause() {
        super.onPause()

        if(::player.isInitialized && player.isPlaying){
            player.pause()
        }
    }


}