package com.example.audioeditor.ui.fragments.library.videoslist

import androidx.lifecycle.ViewModel
import com.example.audioeditor.models.LibraryItemModel

import com.example.audioeditor.repo.AppRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VideoListViewModel(private val appRepo: AppRepo) : ViewModel() {

    private val _libraryList = MutableStateFlow<ArrayList<LibraryItemModel>>(ArrayList())
    val libraryList: StateFlow<ArrayList<LibraryItemModel>> = _libraryList


    fun getFiles() {
        _libraryList.value = appRepo.listVideoFiles()
    }


    fun getSingleVideoFile(pos: Int): LibraryItemModel {
        return appRepo.getSingleVideoFile(pos)
    }

    fun addLibraryItem(libraryItem: LibraryItemModel) {
        _libraryList.value.add(libraryItem)
    }

//    fun createLibraryItemFromPath(path: String): LibraryItemModel {
//        // Extract necessary information from the path or provide default/empty values
//        val id = generateId()  // You need to implement this function to generate an appropriate ID
//        val title: String? = null  // You can set the title based on the path or leave it null
//        val uri: Uri? = null  // You can set the URI based on the path or leave it null
//        val metadata: String? = null  // You can set metadata based on the path or leave it null
//        val extension: String? = null  // You can extract the extension from the path or leave it null
//        val size: String? = null  // You can calculate the size based on the path or leave it null
//        val time: String? = null  // You can calculate the time based on the path or leave it null
//        val albumArt: Bitmap? = null  // You can set the album art based on the path or leave it null
//
//        return LibraryItemModel(id, title, uri, metadata, path, extension, size, time, albumArt)
//    }


}