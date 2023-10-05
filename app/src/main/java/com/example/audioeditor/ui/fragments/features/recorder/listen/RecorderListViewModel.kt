package com.example.audioeditor.ui.fragments.features.recorder.listen

import androidx.lifecycle.ViewModel
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.repo.AppRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecorderListViewModel(private val appRepo: AppRepo) : ViewModel() {

    private val _libraryList = MutableStateFlow<ArrayList<LibraryItemModel>>(ArrayList())
    val libraryList: StateFlow<ArrayList<LibraryItemModel>> = _libraryList


    fun getFiles() {
        _libraryList.value = appRepo.listRecordedAudioFiles()
    }

    fun getSingleAudioFile(pos: Int): LibraryItemModel {
        return appRepo.getSingleRecordedAudioFile(pos)
    }

}