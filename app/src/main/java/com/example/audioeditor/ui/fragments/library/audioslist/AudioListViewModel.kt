package com.example.audioeditor.ui.fragments.library.audioslist

import androidx.lifecycle.ViewModel

import com.example.audioeditor.repo.AppRepo
import com.example.audioeditor.ui.fragments.library.LibraryItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioListViewModel(private val appRepo: AppRepo): ViewModel() {

    private val _libraryList = MutableStateFlow<ArrayList<LibraryItemModel>>(ArrayList())
    val libraryList: StateFlow<ArrayList<LibraryItemModel>> = _libraryList



    fun getFiles() {
     _libraryList.value = appRepo.listAudioFiles()
    }

    fun getSingleFile(pos:Int):LibraryItemModel{
        return appRepo.getSingleFile(pos)
    }





}