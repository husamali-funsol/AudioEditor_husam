package com.example.audioeditor

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