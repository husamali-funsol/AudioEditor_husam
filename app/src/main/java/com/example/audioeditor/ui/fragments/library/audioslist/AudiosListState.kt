package com.example.audioeditor.ui.fragments.library.audioslist

import com.example.audioeditor.ui.fragments.library.LibraryItemModel

sealed class AudioFilesState {
    data class Success(val audioList: List<LibraryItemModel>) : AudioFilesState()
    data class Error(val message: String) : AudioFilesState()
    object Loading : AudioFilesState()
    object Idle : AudioFilesState()
}