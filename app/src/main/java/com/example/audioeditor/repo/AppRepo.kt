package com.example.audioeditor.repo

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.audioeditor.ui.fragments.library.LibraryItemModel
import com.example.audioeditor.utils.formatDuration
import com.example.audioeditor.utils.formatSizeToMB
import com.example.audioeditor.utils.getAlbumArtwork
import com.example.audioeditor.utils.getAudioFileDuration
import com.example.audioeditor.utils.getFileSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRepo(private val appContext: Context) {

    fun listAudioFiles(): ArrayList<LibraryItemModel> {

        val audioItems = mutableListOf<LibraryItemModel>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID // Include the album ID in the projection

            // Add other fields you need
        )

        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs =
            arrayOf("%FunsolAudioEditor%") // Filter for paths containing "FunsolAudioEditor"

        val x = 0
        val xy = x.formatDuration()
// The rest of your code remains the same

        val sortOrder = null // You can specify sorting order here if needed

        Log.d("Debug", "Selection: $selection")
        Log.d("Debug", "SelectionArgs: ${selectionArgs.joinToString()}")

        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )



        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                var title = cursor.getString(titleColumn)
                val uri = Uri.parse(cursor.getString(dataColumn)) // Get the audio URI
                val path = cursor.getString(dataColumn)
                var duration = cursor.getString(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateModified = cursor.getLong(dateModifiedColumn)
                val albumId = cursor.getLong(albumIdColumn)

                // Now, you have the album ID, and you can use it to fetch the album artwork.
                val albumArt = appContext.getAlbumArtwork(albumId)

//                val durationFormatted = (duration.toInt().formatDuration())
                duration = path.getAudioFileDuration().toString()
                val durationFormatted = duration.toInt().formatDuration()

                var sizeMB = "${size.formatSizeToMB()}MB"
                val extension = uri.toString().substringAfterLast(".")
                // Convert dateModified and dateAdded to human-readable format with 12-hour clock, minutes, and seconds with AM/PM
                val modifiedDateTime =
                    SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(
                        Date(dateModified * 1000)
                    )

                val fileNameWithExtension = File(path).name // This gives you "file.txt"
                title = fileNameWithExtension.substringBeforeLast(".") // This gives you "file"

                val sizeInBytes = path.getFileSize()
                sizeMB = "${sizeInBytes.formatSizeToMB()}MB"

                val metadata = "$durationFormatted | $sizeMB | $extension"

                // Create an AudioItemViewModel or a data structure of your choice
                val audioItem =
                    LibraryItemModel(
                        id,
                        title,
                        uri,
                        metadata,
                        path,
                        extension,
                        sizeMB,
                        modifiedDateTime,
                        albumArt
                    )


                Log.d("achaa", "listAudioFiles: $id ")
                Log.d("achaa", "listAudioFiles: $uri ")
                Log.d("achaa", "listAudioFiles: $path ")
                Log.d("achaa", "listAudioFiles: $title ")

                audioItems.add(audioItem)

            }
        }
        return audioItems as ArrayList<LibraryItemModel>
    }


    fun getSingleFile(pos: Int): LibraryItemModel {

        var audioItems: LibraryItemModel? = null

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID // Include the album ID in the projection

            // Add other fields you need
        )

        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs =
            arrayOf("%FunsolAudioEditor%") // Filter for paths containing "FunsolAudioEditor"

        val sortOrder = null // You can specify sorting order here if needed

        Log.d("Debug", "Selection: $selection")
        Log.d("Debug", "SelectionArgs: ${selectionArgs.joinToString()}")

        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            cursor.moveToPosition(pos)

            val id = cursor.getLong(idColumn)
            var title = cursor.getString(titleColumn)
            val uri = Uri.parse(cursor.getString(dataColumn)) // Get the audio URI
            val path = cursor.getString(dataColumn)
            var duration = cursor.getString(durationColumn)
            var size = cursor.getLong(sizeColumn)
            val dateModified = cursor.getLong(dateModifiedColumn)
            val albumId = cursor.getLong(albumIdColumn)

            // Now, you have the album ID, and you can use it to fetch the album artwork.
            val albumArt = appContext.getAlbumArtwork(albumId)

//            val durationFormatted = duration.toInt().formatDuration()
            duration = path.getAudioFileDuration().toString()
            val durationFormatted = duration.toInt().formatDuration()


            var sizeMB = "${size.formatSizeToMB()}MB"
            val extension = uri.toString().substringAfterLast(".")
            // Convert dateModified and dateAdded to human-readable format with 12-hour clock, minutes, and seconds with AM/PM
            val modifiedDateTime =
                SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault()).format(
                    Date(dateModified * 1000)
                )

            val fileNameWithExtension = File(path).name // This gives you "file.txt"
            title = fileNameWithExtension.substringBeforeLast(".") // This gives you "file"

            val sizeInBytes = path.getFileSize()
            sizeMB = "${sizeInBytes.formatSizeToMB()}MB"


            val metadata = "$durationFormatted | $sizeMB | $extension"

            // Create an AudioItemViewModel or a data structure of your choice
            val audioItem =
                LibraryItemModel(
                    id,
                    title,
                    uri,
                    metadata,
                    path,
                    extension,
                    sizeMB,
                    modifiedDateTime,
                    albumArt
                )


            audioItems = audioItem

        }
        return audioItems!!
    }
}