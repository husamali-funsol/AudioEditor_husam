package com.example.audioeditor.models

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class LibraryItemModel(
    val id: Long,
    var title: String?,
    var uri: Uri?,
    val metadata: String?,
    var path: String?,
    val extension: String?,
    var size: String?,
    var time: String?,
    val albumArt: Bitmap? = null // Include album artwork as a Bitmap property
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString(),
        parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(Bitmap::class.java.classLoader) // Read album artwork as a Parcelable Bitmap
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeParcelable(uri, flags)
        parcel.writeString(metadata)
        parcel.writeString(path)
        parcel.writeString(extension)
        parcel.writeString(size)
        parcel.writeString(time)
        parcel.writeParcelable(albumArt, flags) // Write album artwork as a Parcelable Bitmap
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LibraryItemModel> {
        override fun createFromParcel(parcel: Parcel): LibraryItemModel {
            return LibraryItemModel(parcel)
        }

        override fun newArray(size: Int): Array<LibraryItemModel?> {
            return arrayOfNulls(size)
        }
    }
}
