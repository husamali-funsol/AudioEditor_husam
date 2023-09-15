package com.example.audioeditor.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.example.audioeditor.ui.fragments.library.LibraryItemModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit




    fun Context.getAlbumArtwork(albumId: Long): Bitmap? {
        val albumArtworkUri = Uri.parse("content://media/external/audio/albumart")
        val albumArtworkUriWithAlbumId = ContentUris.withAppendedId(albumArtworkUri, albumId)
        try {
            val inputStream =
                this.contentResolver.openInputStream(albumArtworkUriWithAlbumId)
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun Int.formatDuration(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(this.toLong())
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this.toLong()) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this.toLong()) % 60

        // Check if hours is zero, and format accordingly
        val hoursStr = if (hours > 0) {
            String.format("%02d:", hours)
        } else {
            "" // If hours is zero, don't display it
        }

        // Format minutes and seconds as usual
        val minutesStr = String.format("%02d:", minutes)
        val secondsStr = String.format("%02d", seconds)

        return "$hoursStr$minutesStr$secondsStr"
    }

    fun Long.formatSizeToMB(): String {
        val sizeKB = this / 1024.0
        val sizeMB = sizeKB / 1024.0
        return String.format("%.2f", sizeMB) // Format size with two decimal places
    }

fun Context.scanFiles(file: File) {
    MediaScannerConnection.scanFile(
        this,
        arrayOf(file.absolutePath),
        null
    ) { _, _ -> }
}

fun Context.refreshMediaStore(file: File) {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    mediaScanIntent.data = Uri.fromFile(file)
    this.sendBroadcast(mediaScanIntent)
}



fun Context.refreshMediaStoreForAudioFiles() {
    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    mediaScanIntent.data = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
    this.sendBroadcast(mediaScanIntent)
}

fun Long.convertMillisToMinutes(): String {
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    return String.format("%02d:%02d", minutes, seconds)
}





