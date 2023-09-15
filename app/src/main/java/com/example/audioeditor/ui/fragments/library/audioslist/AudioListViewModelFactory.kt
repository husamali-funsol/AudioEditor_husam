package com.example.audioeditor.ui.fragments.library.audioslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.audioeditor.repo.AppRepo

@Suppress("UNCHECKED_CAST")
class AudioListViewModelFactory(private val appRepo: AppRepo): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioListViewModel::class.java)) {
            return AudioListViewModel(appRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}