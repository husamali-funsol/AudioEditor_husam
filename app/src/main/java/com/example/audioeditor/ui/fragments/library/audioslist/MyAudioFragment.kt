package com.example.audioeditor.ui.fragments.library.audioslist

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audioeditor.R
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.DetailsBottomSheetDialogBinding
import com.example.audioeditor.databinding.FragmentMyAudioBinding
import com.example.audioeditor.databinding.LibraryBottomSheetDialogBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.repo.AppRepo
import com.example.audioeditor.ui.fragments.library.LibraryItemAdapter
import com.example.audioeditor.ui.fragments.library.LibraryItemModel
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayList


class MyAudioFragment(val callBack: (List<LibraryItemModel>, Int) -> Unit) : Fragment() {

    private lateinit var binding: FragmentMyAudioBinding

    private var alertDialog: AlertDialog? = null

    private lateinit var libraryBottomSheetDialogBinding: LibraryBottomSheetDialogBinding
    private lateinit var detailsBottomSheetDialogBinding: DetailsBottomSheetDialogBinding
    private lateinit var renameDialogBinding: RenameDialogBinding
    private lateinit var deleteDialogBinding: DeleteDialogBinding

    private lateinit var audioUri: Uri

    private lateinit var appRepo: AppRepo
    private lateinit var viewModel: AudioListViewModel
    private lateinit var adapter: LibraryItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMyAudioBinding.inflate(inflater, container, false)
        appRepo = AppRepo(requireContext())
        val viewModelFactory = AudioListViewModelFactory(appRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[AudioListViewModel::class.java]

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireContext().refreshMediaStoreForAudioFiles()
//        listAudioFiles()

        binding.rvMyAudio.layoutManager = LinearLayoutManager(requireContext())
        adapter = LibraryItemAdapter(ArrayList(), ::showOptions, ::navigateToPlayer)
        binding.rvMyAudio.adapter = adapter

        getList()

        requireContext().refreshMediaStoreForAudioFiles()
        viewModel.getFiles()
    }

    private fun getList() {
        lifecycleScope.launch {
            viewModel.libraryList.collect { libraryList ->
                submitNewList(libraryList)
            }
        }
    }

    private fun submitNewList(libraryList: ArrayList<LibraryItemModel>) {
        adapter.submitNewList(libraryList)
    }

    private fun navigateToPlayer(libList: List<LibraryItemModel>, position: Int) {
        callBack.invoke(libList, position)
    }


    private fun showOptions(libItem: LibraryItemModel, position: Int) {
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
            val alertDialogBuilder =
                AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogStyle)

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
                Log.d("Debug", "entered text: $enteredText")
                Log.d("Debug", "ext: $ext")
                renameFile(enteredText, ext!!, libItem, position)

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

            audioUri = libItem.uri!!

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
                    libItem.path
                val originalFile = File(filePath!!)

                if (originalFile.exists()) {
                    originalFile.delete()
                    requireContext().scanFiles(originalFile)

                }

                requireContext().refreshMediaStoreForAudioFiles()
                getList()
                alertDialog?.dismiss()
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

    private fun renameFile(newName: String, ext: String, libItem: LibraryItemModel, position: Int) {
        val filePath =
            libItem!!.path
//        val newFileName = "new_audio_file.wav" // Provide the new file name
        val newFileName = "$newName.$ext" // Provide the new file name
        Log.d("library rename", newFileName)

        // Create a File object for the original file
        val originalFile = File(filePath!!)

        if (!originalFile.exists()) {
            Log.e("asdf", "File does not exist at path: $filePath")
            return
        }

        // Create a File object for the new file with the desired name
        val directoryPath = originalFile.parentFile // Get the directory path
        val newFile = File(directoryPath, newFileName)


        // Rename the file
        if (originalFile.exists()) {
            if (originalFile.renameTo(newFile)) {
                // File renamed successfully
                val contentResolver = requireContext().contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                contentValues.put(MediaStore.MediaColumns.TITLE, newName)

                audioUri = libItem.uri!!

                val currentTimeMillis = System.currentTimeMillis()
                newFile.setLastModified(currentTimeMillis)

                // Construct a content URI for the original file using its ID
                val originalFileId = libItem.id // Assuming you have the ID of the media file
                val originalUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    originalFileId
                )

                // Update the path and URI in your LibraryItemViewModel
                libItem.path = newFile.path
                libItem.uri = originalUri // Use the content URI

                val newPath =
                    libItem.path

                val updatedFile = File(newPath!!)
                requireContext().refreshMediaStore(updatedFile)

                val selection = "${MediaStore.MediaColumns.DATA} = ?"
                val selectionArgs = arrayOf(newPath)

                Log.d("asdf", "Original URI: $originalUri")

                contentResolver.update(originalUri, contentValues, selection, selectionArgs)

                // Refresh the MediaStore to reflect the changes
                requireContext().refreshMediaStore(updatedFile)

                Toast.makeText(requireContext(), "Renaming Successful", Toast.LENGTH_SHORT).show()

                //viewModel.getFiles()

                adapter.itemUpdated(position, viewModel.getSingleFile(position))

            } else {
                // Failed to rename the file
                // Handle the error accordingly
                Toast.makeText(requireContext(), "Renaming Failed", Toast.LENGTH_SHORT).show()

            }
        } else {
            // The original file does not exist
            Toast.makeText(requireContext(), "Original File does not exist", Toast.LENGTH_SHORT)
                .show()
        }

        val newPath =
            libItem.path

        val updatedFile = File(newPath!!)
        requireContext().refreshMediaStore(updatedFile)

        requireContext().refreshMediaStoreForAudioFiles()
        getList()
    }

}