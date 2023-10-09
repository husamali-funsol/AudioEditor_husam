package com.example.audioeditor.ui.fragments.library.videoslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.audioeditor.repo.AppRepo

@Suppress("UNCHECKED_CAST")
class VideoListViewModelFactory(private val appRepo: AppRepo): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoListViewModel::class.java)) {
            return VideoListViewModel(appRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}