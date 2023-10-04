package com.example.audioeditor.ui.fragments.library.audioplayer

import com.example.audioeditor.models.LibraryItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlayerViewModel {
    private val _audiosList = MutableStateFlow<ArrayList<LibraryItemModel>>(ArrayList())
    val audioList: StateFlow<ArrayList<LibraryItemModel>> = _audiosList


}