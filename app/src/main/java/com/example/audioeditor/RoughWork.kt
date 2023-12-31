package com.example.audioeditor

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.utils.getStorageDir
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.showSmallLengthToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//    private fun renameFile(newName: String, ext: String, libItem: LibraryItemModel) {
//        val filePath =
//            libItem!!.path
////        val newFileName = "new_audio_file.wav" // Provide the new file name
//        val newFileName = "$newName.$ext" // Provide the new file name
//        Log.d("library rename", newFileName)
//
//        // Create a File object for the original file
//        val originalFile = File(filePath!!)
//
//        // Create a File object for the new file with the desired name
//        val directoryPath = originalFile.parentFile // Get the directory path
//        val newFile = File(directoryPath, newFileName)
//
//        // Rename the file
//        if (originalFile.exists()) {
//            if (originalFile.renameTo(newFile)) {                // File renamed successfully
//                val contentResolver = requireContext().contentResolver
//                val contentValues = ContentValues()
//                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
//                contentValues.put(MediaStore.MediaColumns.TITLE, newName)
//
//                audioUri = libItem.uri!!
//
//                val currentTimeMillis = System.currentTimeMillis()
//                newFile.setLastModified(currentTimeMillis)
//
//                // Construct a content URI for the original file using its ID
//                val originalFileId = libItem.id // Assuming you have the ID of the media file
//                val originalUri = ContentUris.withAppendedId(
//                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                    originalFileId
//                )
//
//                // Update the path and URI in your LibraryItemViewModel
//                libItem.path = newFile.path
//                libItem.uri = originalUri // Use the content URI
//
//                val newPath =
//                    libItem!!.path
//
//                val updatedFile = File(newPath!!)
//                requireContext().refreshMediaStore(updatedFile)
//
//                val selection = "${MediaStore.MediaColumns.DATA} = ?"
//                val selectionArgs = arrayOf(newPath)
//
//                contentResolver.update(originalUri, contentValues, selection, selectionArgs)
//
//                // Refresh the MediaStore to reflect the changes
//                requireContext().refreshMediaStore(updatedFile)
//
//                Toast.makeText(requireContext(), "Renaming Successful", Toast.LENGTH_SHORT).show()
////                scanFiles(updatedFile)
////                listAudioFiles()
////                scanFiles(newFile)
//
//            } else {
//                // Failed to rename the file
//                // Handle the error accordingly
//                Toast.makeText(requireContext(), "Renaming Failed", Toast.LENGTH_SHORT).show()
//
//            }
//        } else {
//            // The original file does not exist
//            Toast.makeText(requireContext(), "Original File does not exist", Toast.LENGTH_SHORT)
//                .show()
//            // Handle this case as needed
//        }
//
//        // If you want to delete the original file (optional)
////        originalFile.delete()
//
//        val newPath =
//            libItem!!.path
//
//        val updatedFile = File(newPath!!)
//        requireContext().refreshMediaStore(updatedFile)
//
//
//
//        requireContext().refreshMediaStoreForAudioFiles()
//        //setAudioFileData()
//        binding.tvTitleLP.text = updatedFile.name
//
//    }







//private fun showOptions(libItem: LibraryItemModel, position: Int) {
////        val bottomSheet = BottomSheetDialog(requireContext())
//    val parent = libraryBottomSheetDialogBinding.root.parent as? ViewGroup
//    parent?.removeView(libraryBottomSheetDialogBinding.root)
//    bottomSheet.setContentView(libraryBottomSheetDialogBinding.root)
//
//    if (libItem != null) {
//        libraryBottomSheetDialogBinding.tvTitleLibSheet.text = libItem!!.title
//        libraryBottomSheetDialogBinding.tvMetaDataLibSheet.text = libItem!!.metadata
//    }
//
//    libraryBottomSheetDialogBinding.tvRenameLibSheet.setOnClickListener {
////                renameDialogBinding = RenameDialogBinding.inflate(layoutInflater)
//        showRenameDialog(libItem, position)
//    }
//
//    libraryBottomSheetDialogBinding.tvDetailLibSheet.setOnClickListener {
//        showDetailsBottomSheet(libItem, position)
//    }
//
//
//    libraryBottomSheetDialogBinding.tvDeleteLibSheet.setOnClickListener {
////                deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
//        showDeleteDialog(libItem, position)
//
//    }
//    bottomSheet.show()
//}















//
//private fun showFragmentViews(){
//
//    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
//        // Perform your tasks on a background thread
//        withContext(Dispatchers.Main) {
//            binding.loader.visibility = View.VISIBLE
//            binding.recyclerView.visibility = View.GONE
//            binding.ivNoAudio.visibility= View.GONE
//            binding.tvNoAudio.visibility= View.GONE
//        }
//        // Fetch the list in the background
//        getList()
//        context?.refreshMediaStoreForAudioFiles()
//        viewModel.getFiles()
//
//        withContext(Dispatchers.Main) {
//            binding.loader.visibility = View.GONE
//
//            if (adapter.itemCount == 0) {
//                binding.loader.visibility = View.GONE
//                binding.ivNoAudio.visibility= View.VISIBLE
//                binding.tvNoAudio.visibility= View.VISIBLE
//                binding.recyclerView.visibility = View.GONE
//            } else {
//                binding.loader.visibility = View.GONE
//                binding.recyclerView.visibility = View.VISIBLE
//                binding.ivNoAudio.visibility= View.GONE
//                binding.tvNoAudio.visibility= View.GONE
//            }
//        }
//
//    }
//
//
//}
//
//private fun hideFragmentViews(){
//    binding.loader.visibility = View.VISIBLE
//
//    if (adapter.itemCount == 0) {
////            binding.loader.visibility = View.VISIBLE
////            binding.ivNoAudio.visibility= View.GONE
////            binding.tvNoAudio.visibility= View.GONE
//        binding.recyclerView.visibility = View.VISIBLE
//    } else {
//        binding.loader.visibility = View.VISIBLE
//        binding.recyclerView.visibility = View.GONE
////            binding.ivNoAudio.visibility= View.VISIBLE
////            binding.tvNoAudio.visibility= View.VISIBLE
//    }
//}













//private fun renameAndMoveFile(newName: String, ext: String, libItem: LibraryItemModel, position: Int) {
//    var filePath: String? = null
//    if(lastFunctionCalled == null){
//        filePath = libItem.path
//    }
//    else{
//        when(lastFunctionCalled){
//            "speed" -> filePath = outputPathSpeed
//            "trim" -> filePath = outputPathTrim
//        }
//    }
//    val newFileName = "$newName.$ext" // Provide the new file name
//    val originalFile = File(filePath!!)
//    // Create a File object for the new file with the desired name
//    val outputDir = getStorageDir()
//    val recorderFolder = File(outputDir, "FunsolAudioRecorder")
////        val directoryPath = originalFile.parentFile // Get the directory path
//    val newFile = File(recorderFolder, newFileName)
//
//    // Rename the file
//    if (originalFile.exists()) {
//        if (originalFile.renameTo(newFile)) {
//
//            val newPath =
//                newFile.path
//            val updatedFile = File(newPath!!)
//
//            val currentTimeMillis = System.currentTimeMillis()
//            newFile.setLastModified(currentTimeMillis)
//            originalFile.setLastModified(currentTimeMillis)
//            libItem.time =  SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(
//                Date(currentTimeMillis)
//            )
//            val fileSizeInBytes = newFile.length()
//            val fileSizeInMB = fileSizeInBytes / (1024 * 1024)
//
//            libItem.size = "${fileSizeInMB}MB"
//            // Refresh the MediaStore to reflect the changes
//            context?.refreshMediaStore(updatedFile)
//            context?.showSmallLengthToast("Renaming Successful")
////                adapter.itemUpdated(position, viewModel.getSingleFile(position))
//
//        } else {
//            // Failed to rename the file
//            // Handle the error accordingly
//            context?.showSmallLengthToast("Renaming Failed")
//        }
//    } else {
//        // The original file does not exist
//        context?.showSmallLengthToast("Original File does not exist")
//    }
//
//    val newPath =
//        newFile.path
//
//    val updatedFile = File(newPath)
//    libItem.path = newPath
//    context?.refreshMediaStore(updatedFile)
//
//    context?.refreshMediaStoreForAudioFiles()
////        getList()
//
//    val fileNameWithExtension = updatedFile.name // This gives you "file.txt"
//    binding.tvFragmentTitle.text = fileNameWithExtension.substringBeforeLast(".")
//    libItem.title = fileNameWithExtension.substringBeforeLast(".")
//}
