package com.example.audioeditor.ui.fragments.library.audioplayer

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.audioeditor.R
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.DetailsBottomSheetDialogBinding
import com.example.audioeditor.databinding.FragmentMyAudioPlayerBinding
import com.example.audioeditor.databinding.LibraryBottomSheetDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.ui.fragments.library.LibraryItemModel
import com.example.audioeditor.utils.convertMillisToMinutes
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File


class MyAudioPlayerFragment : Fragment() {

    private lateinit var binding: FragmentMyAudioPlayerBinding

    private lateinit var libraryBottomSheetDialogBinding: LibraryBottomSheetDialogBinding
    private lateinit var detailsBottomSheetDialogBinding: DetailsBottomSheetDialogBinding
    private lateinit var renameDialogBinding: RenameDialogBinding
    private lateinit var deleteDialogBinding: DeleteDialogBinding

    lateinit var mediaPlayer: MediaPlayer

    private val updateSeekBarHandler = Handler()

    private lateinit var audioUri: Uri
    private var libItem: LibraryItemModel? = null

    var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMyAudioPlayerBinding.inflate(layoutInflater, container, false)
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
        createMediaPlayer(audioUri)


        binding.ibBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivSkipNextLP.setOnClickListener {
            position = position!! + 1

            if (position != null && position!! >= 0 && position!! < (mList?.size ?: 0))
                {
                libItem = mList?.get(position!!)
            }
            if (libItem != null) {
                audioUri = libItem!!.uri!!
                binding.tvTitleLP.text = libItem!!.title
                binding.tvMetadataLP.text = libItem!!.metadata
                // Use Glide to load and set the image
                Glide.with(requireContext())
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.music_thumbnail) // Placeholder image
                    .error(R.drawable.music_thumbnail) // Error image
                    .into(binding.imageView) // Set the ImageView you want to load the image into
                // Use Glide to load and set the image
                Glide.with(requireContext())
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.placeholder_image) // Placeholder image
                    .error(R.drawable.placeholder_image) // Error image
                    .into(binding.ivAvatarLibItem) // Set the ImageView you want to load the image into

                binding.ivPlayerLP.setImageResource(R.drawable.play_circle)


                Log.d("get path", libItem!!.path.toString())
            }
//
            createMediaPlayer(audioUri)
        }

        binding.ivSkipPrevLP.setOnClickListener {
            position = position!! - 1

            if (position != null && position!! >= 0 && position!! < (mList?.size ?: 0))
            {
                libItem = mList?.get(position!!)
            }
            if (libItem != null) {
                audioUri = libItem!!.uri!!
                binding.tvTitleLP.text = libItem!!.title
                binding.tvMetadataLP.text = libItem!!.metadata
                // Use Glide to load and set the image
                Glide.with(requireContext())
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.music_thumbnail) // Placeholder image
                    .error(R.drawable.music_thumbnail) // Error image
                    .into(binding.imageView) // Set the ImageView you want to load the image into
                // Use Glide to load and set the image
                Glide.with(requireContext())
                    .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                    .placeholder(R.drawable.placeholder_image) // Placeholder image
                    .error(R.drawable.placeholder_image) // Error image
                    .into(binding.ivAvatarLibItem) // Set the ImageView you want to load the image into

                binding.ivPlayerLP.setImageResource(R.drawable.play_circle)


                Log.d("get path", libItem!!.path.toString())
            }
//
            createMediaPlayer(audioUri)
        }


        binding.ivPlayerLP.setOnClickListener {
            if (!(mediaPlayer.isPlaying)) {
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.start()
                    binding.ivPlayerLP.setImageResource(R.drawable.pause_circle)
                    updateSeekBar()
                }
            } else {
                if (::mediaPlayer.isInitialized) {
                    mediaPlayer.pause()
                    binding.ivPlayerLP.setImageResource(R.drawable.play_circle)
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

        binding.ibMoreLP.setOnClickListener {
            val bottomSheet = BottomSheetDialog(requireContext())
            libraryBottomSheetDialogBinding =
                LibraryBottomSheetDialogBinding.inflate(layoutInflater)
            bottomSheet.setContentView(libraryBottomSheetDialogBinding.root)

            if (libItem != null) {
                libraryBottomSheetDialogBinding.tvTitleLibSheet.text = libItem!!.title
                libraryBottomSheetDialogBinding.tvMetaDataLibSheet.text = libItem!!.metadata
            }

            libraryBottomSheetDialogBinding.tvRenameLibSheet.setOnClickListener {
//                renameDialogBinding = RenameDialogBinding.inflate(layoutInflater)
                val alertDialogBuilder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

                renameDialogBinding = RenameDialogBinding.inflate(layoutInflater)
                val dialogView = renameDialogBinding.root
                alertDialogBuilder.setView(dialogView)

                if (libItem != null) {
                    // Set the initial text in your EditText (if needed)
                    renameDialogBinding.etRenameRD.setText(libItem!!.title)
                    renameDialogBinding.etRenameRD.setSelection(renameDialogBinding.etRenameRD.length())//placing cursor at the end of the text

                }

                renameDialogBinding.tvConfirmRD.setOnClickListener {
                    // Handle the positive button click event here
                    // You can retrieve the text entered in the EditText like this:
                    val enteredText = renameDialogBinding.etRenameRD.text.toString()

                    // Implement your logic here (e.g., renameFile(enteredText))
                    val ext = libItem!!.extension
                    renameFile(enteredText, ext!!, libItem!!)

                    alertDialog?.dismiss()
                }

                renameDialogBinding.tvCancelRD.setOnClickListener {
                    // Handle the negative button click event here
                    // This is where you can cancel the dialog if needed
                    alertDialog?.dismiss()

                }

                alertDialog = alertDialogBuilder.create()
                bottomSheet.dismiss()
                alertDialog!!.show()
            }

            libraryBottomSheetDialogBinding.tvDetailLibSheet.setOnClickListener {
                val detailsBottomSheet = BottomSheetDialog(requireContext())
                detailsBottomSheetDialogBinding =
                    DetailsBottomSheetDialogBinding.inflate(layoutInflater)
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

            libraryBottomSheetDialogBinding.tvShareLibSheet.setOnClickListener {

                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "audio/*"

                shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri)

                startActivity(Intent.createChooser(shareIntent, "Share Audio"))
            }

            libraryBottomSheetDialogBinding.tvDeleteLibSheet.setOnClickListener {
//                deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
                val alertDialogBuilder =
                    AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

                deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
                val dialogView = deleteDialogBinding.root
                alertDialogBuilder.setView(dialogView)


                deleteDialogBinding.tvDeleteBtnDD.setOnClickListener {
                    // Handle the positive button click event here
                    val filePath =
                        libItem?.path
                    val originalFile = File(filePath!!)

                    if (originalFile.exists()) {
                        originalFile.delete()
                        requireContext().scanFiles(originalFile)

                    }

                    requireContext().refreshMediaStoreForAudioFiles()

                    alertDialog?.dismiss()
                    findNavController().popBackStack()
                }

                deleteDialogBinding.tvCancelDD.setOnClickListener {
                    alertDialog?.dismiss()
                }


                alertDialog = alertDialogBuilder.create()
                bottomSheet.dismiss()
                alertDialog!!.show()

            }
            bottomSheet.show()
        }
    }

    private fun setAudioFileData(){
        if (libItem != null) {
            audioUri = libItem!!.uri!!
            binding.tvTitleLP.text = libItem!!.title
            binding.tvMetadataLP.text = libItem!!.metadata
            // Use Glide to load and set the image
            Glide.with(requireContext())
                .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                .placeholder(R.drawable.music_thumbnail) // Placeholder image
                .error(R.drawable.music_thumbnail) // Error image
                .into(binding.imageView) // Set the ImageView you want to load the image into
            // Use Glide to load and set the image
            Glide.with(requireContext())
                .load(libItem!!.albumArt) // Replace with the appropriate URL or resource
                .placeholder(R.drawable.placeholder_image) // Placeholder image
                .error(R.drawable.placeholder_image) // Error image
                .into(binding.ivAvatarLibItem) // Set the ImageView you want to load the image into


            Log.d("get path", libItem!!.path.toString())
        }
    }

    private fun renameFile(newName: String, ext: String, libItem: LibraryItemModel) {
        val filePath =
            libItem!!.path
//        val newFileName = "new_audio_file.wav" // Provide the new file name
        val newFileName = "$newName.$ext" // Provide the new file name
        Log.d("library rename", newFileName)

        // Create a File object for the original file
        val originalFile = File(filePath!!)

        // Create a File object for the new file with the desired name
        val directoryPath = originalFile.parentFile // Get the directory path
        val newFile = File(directoryPath, newFileName)

        // Rename the file
        if (originalFile.exists()) {
            if (originalFile.renameTo(newFile)) {                // File renamed successfully
                val contentResolver = requireContext().contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                contentValues.put(MediaStore.MediaColumns.TITLE, newName)

                audioUri = libItem.uri!!

                val currentTimeMillis = System.currentTimeMillis()
                newFile.setLastModified(currentTimeMillis)

                // Construct a content URI for the original file using its ID
                val originalFileId = libItem.id // Assuming you have the ID of the media file
                val originalUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, originalFileId)

                // Update the path and URI in your LibraryItemViewModel
                libItem.path = newFile.path
                libItem.uri = originalUri // Use the content URI

                val newPath =
                    libItem!!.path

                val updatedFile = File(newPath!!)
                requireContext().refreshMediaStore(updatedFile)

                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                val selectionArgs = arrayOf(newPath)

                contentResolver.update(originalUri, contentValues, selection, selectionArgs)

                // Refresh the MediaStore to reflect the changes
                requireContext().refreshMediaStore(updatedFile)


                Log.d("kkkkiiio", "renameFile: ${updatedFile.name}")

                Toast.makeText(requireContext(), "Renaming Successful", Toast.LENGTH_SHORT).show()
//                scanFiles(updatedFile)
//                listAudioFiles()
//                scanFiles(newFile)

            } else {
                // Failed to rename the file
                // Handle the error accordingly
                Toast.makeText(requireContext(), "Renaming Failed", Toast.LENGTH_SHORT).show()

            }
        } else {
            // The original file does not exist
            Toast.makeText(requireContext(), "Original File does not exist", Toast.LENGTH_SHORT).show()
            // Handle this case as needed
        }

        // If you want to delete the original file (optional)
//        originalFile.delete()

        val newPath =
            libItem!!.path

        val updatedFile = File(newPath!!)
        requireContext().refreshMediaStore(updatedFile)



        requireContext().refreshMediaStoreForAudioFiles()
        //setAudioFileData()
        binding.tvTitleLP.text = updatedFile.name

    }

    private fun createMediaPlayer(filepath: Uri) {
        val uri = Uri.fromFile(File(filepath.toString()))

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), uri)
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

    override fun onDestroy() {
        super.onDestroy()

        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)

        }
    }

}