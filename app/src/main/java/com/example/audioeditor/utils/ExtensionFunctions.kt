package com.example.audioeditor.utils

import android.content.ContentUris
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.audioeditor.interfaces.CommandExecutionCallback
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


fun View.setOnOneClickListener(debounceTime: Long = 600L, action: () -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
            else action()

            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}


fun String.getVideoThumbnail(): Bitmap? { //videopath.getvideothumbnail
    val thumbnailPath = this.replace(".mp4", "_thumbnail.jpg") // Example: Replace with your naming convention
    val thumbnailFile = File(thumbnailPath)

    if (thumbnailFile.exists()) {
        return BitmapFactory.decodeFile(thumbnailFile.absolutePath)
    }

    return null // Return null if thumbnail doesn't exist
}


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
//        val hoursStr = if (hours > 0) {
//            String.format("%02d:", hours)
//        } else {
//            "" // If hours is zero, don't display it
//        }

        // Format minutes and seconds as usual
        val hoursStr = String.format("%02d:", hours)
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

fun Context.scanDirectory() {
    val storageDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "FunsolAudioEditor"
    )
    MediaScannerConnection.scanFile(
        this,
        arrayOf(storageDir.absolutePath),
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
    mediaScanIntent.data = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
    this.sendBroadcast(mediaScanIntent)
}

fun Long.convertMillisToMinutes(): String {
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    return String.format("%02d:%02d", minutes, seconds)
}

fun Uri.getFileSize(): String {
    val file = File(this.path!!)
    return file.length().formatSizeToMB()
}

fun MediaPlayer.calculateProgress(): Float {
    val audioDurationMillis = this.duration
    val currentPositionMillis = this.currentPosition
    return (currentPositionMillis.toFloat() / audioDurationMillis) * 100
}

fun Array<String>.executeCommand(callback: CommandExecutionCallback) {
    try {
        FFmpegKit.executeAsync(
            this.joinToString(" ")
        ) { session ->
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                // FFmpeg command was executed successfully
                Log.d("AudioEditor", "Command Execution Successful")

                val outputText = session.output
                Log.d("AudioEditor", "Output: $outputText")


                Handler(Looper.getMainLooper()).post {
                    callback.onCommandExecutionSuccess()
                }

            } else {
                // FFmpeg command execution failed
                Log.d("AudioEditor", "Command Execution Failed")

                val outputText = session.output
                Log.d("AudioEditor", "Output: $outputText")

                Handler(Looper.getMainLooper()).post {
                    callback.onCommandExecutionFailure("Command Execution Failed")
                }
            }


        }
    } catch (e: Exception) {
        Log.d("AudioEditor", "exception: ${e.toString()}")
        callback.onCommandExecutionFailure("Exception: ${e.toString()}")

    }
}

fun Context.getInputPath(audioUri: Uri): String {
    val audioInputStream = contentResolver.openInputStream(audioUri)
    var audioFilePath = ""
    if (audioInputStream != null) {
        // Create a temporary file to copy the audio data
        val tempAudioFile = this.createTempAudioFile()


        // Copy the audio data from the input stream to the temporary file
        val outputStream = FileOutputStream(tempAudioFile)
        audioInputStream.copyTo(outputStream)

        // Close the streams
        outputStream.close()
        audioInputStream.close()

        // Get the path of the temporary audio file
        audioFilePath = tempAudioFile.absolutePath
        Log.d("TAG", audioFilePath.toString())

    }

    return audioFilePath
}

fun Context.createTempAudioFile(): File {
    // Create a temporary file in the app's private directory
    val tempFileName = "temp_audio.mp3"
    return File(filesDir, tempFileName)
}

fun getCurrentTimestampString(): String {
    val currentTimeMillis = System.currentTimeMillis()
    val sdf = SimpleDateFormat("HH_mm_ss_SSS")
    val currentDate = Date(currentTimeMillis)
    return sdf.format(currentDate)
}

fun String.getOutputFile(ext: String): File {
//        val storageDir = File(Environment.getExternalStorageDirectory(), "FunsolAudioEditor")
    val storageDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "FunsolAudioEditor"
    )

    val ext = ext.lowercase()

    if (!storageDir.exists()) {
        storageDir.mkdirs() // Create the directory if it doesn't exist
    }

//    val fileName = "audio_editor_${getCurrentTimestampString()}.$ext"

    val fileName = "${this}.$ext"
    return File(storageDir, fileName)
}


fun getStorageDir(): String {
//        val storageDir = File(Environment.getExternalStorageDirectory(), "FunsolAudioEditor")
    val storageDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "FunsolAudioEditor"
    )

//    val ext = ext.lowercase()

    if (!storageDir.exists()) {
        storageDir.mkdirs() // Create the directory if it doesn't exist
    }

//    val fileName = "audio_editor_${getCurrentTimestampString()}.$ext"

//    val fileName = "${this}.$ext"
    return storageDir.path
}

//@RequiresApi(Build.VERSION_CODES.O)
fun Context.performHapticFeedback() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(VIBRATOR_SERVICE) as Vibrator
    }
    if (this.hasVibrationPermission()) {
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        vibrator.vibrate(effect)
    }
}

fun Context.hasVibrationPermission(): Boolean {
    val permission = android.Manifest.permission.VIBRATE
    val result = ContextCompat.checkSelfPermission(
        this,
        permission
    )
    return result == PackageManager.PERMISSION_GRANTED
}


fun String.getFileSize(): Long {
    val file = File(this)
    return if (file.exists()) {
        file.length()
    } else {
        -1 // Return -1 if the file doesn't exist
    }
}

fun String.getAudioFileDuration(): Long {
    val mediaPlayer = MediaPlayer()
    return try {
        if (this.isNotEmpty()) {
            mediaPlayer.setDataSource(this)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong()
            duration
        } else {
            // Handle invalid input (empty or null path)
            -1
        }
    } catch (e: IOException) {
        // Log the error or handle it as needed
        -1
    } catch (e: IllegalStateException) {
        // Log the error or handle it as needed
        -1
    } finally {
        mediaPlayer.release()  // Ensure MediaPlayer is released
    }
}

fun String.getVideoFileDuration(): Long {
    val mediaPlayer = MediaPlayer()

    return try {
        if (this.isNotEmpty()) {
            mediaPlayer.setDataSource(this)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong()
            duration
        } else {
            // Handle invalid input (empty or null path)
            -1
        }
    } catch (e: IOException) {
        // Log the error or handle it as needed
        -1
    } catch (e: IllegalStateException) {
        // Log the error or handle it as needed
        -1
    } finally {
        mediaPlayer.release()  // Ensure MediaPlayer is released
    }
}


fun Context.getFileNameFromUri(uri: Uri): String? {
    val cursor = this?.contentResolver?.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex =
                it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                return it.getString(displayNameIndex).substringBeforeLast(".")
            }
        }
    }
    return null
}


fun Context.getExtensionFromUri(uri: Uri): String? {
    val cursor = this?.contentResolver?.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex =
                it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                return it.getString(displayNameIndex).substringAfterLast(".")
            }
        }
    }
    return null
}

fun String.replaceSpaceWithUnderscore(): String{
    return this.lowercase(Locale.getDefault()).replace(' ', '_').trim()
}

fun Context.showSmallLengthToast(text: String) {
    Handler(Looper.getMainLooper()).post{
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}

fun Context.showLongLengthToast(text: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}

fun dismissDialog(alertDialog: AlertDialog?, dialogView: ConstraintLayout?){
    alertDialog?.dismiss()
    val parentView = dialogView?.parent as? ViewGroup
    parentView?.removeView(dialogView)
}


fun String.getUriFromPath(): Uri? {
    val file = File(this)
    return Uri.fromFile(file)
}

fun Array<String>.executeCommandInSeries( callback: (Boolean) -> Unit) {
    try {
        FFmpegKit.executeAsync(
            this.joinToString(" ")
        ) { session ->
            val state = session.state
            val returnCode = session.returnCode

            if (ReturnCode.isSuccess(returnCode)) {
                Log.d("AudioEditor", "FFmpeg Successful")

                val outputText = session.output
                Log.d("AudioEditor", "Output: $outputText")
                // Call the callback with success
                callback(true)
            } else {
                // FFmpeg command execution failed
                Log.d(
                    "AudioEditor",
                    String.format(
                        "Command failed with state %s and rc %s.%s",
                        state,
                        returnCode,
                        session.failStackTrace
                    )
                )
                val outputText = session.output
                Log.d("AudioEditor", "Output: $outputText")
                // Call the callback with failure
                callback(false)
            }
        }
    } catch (e: Exception) {
        Log.d("AudioEditor", "exception: ${e.toString()}")
        // Call the callback with failure
        callback(false)
    }
}


fun deleteFile(f1: File) {

    val file1 = f1

// Check if the files exist before attempting to delete them
    if (file1.exists()) {
        val deleted1 = file1.delete()
        if (deleted1) {
            println("File ${f1.path} deleted successfully.")
        } else {
            println("Failed to delete ${f1.path}.")
        }
    } else {
        println("File ${f1.path} does not exist.")
    }

}

fun Context.getTemporaryFileInPrivateDirectory(ext: String): File {
    val outputDir =
        getExternalFilesDir(Environment.DIRECTORY_MUSIC) // Create a directory within the app's private directory
    outputDir?.mkdirs()

    val fileName =
        "temp_audio_${getCurrentTimestampString()}.${ext}" // Replace with your desired filename and extension
    return File(outputDir, fileName)
}











