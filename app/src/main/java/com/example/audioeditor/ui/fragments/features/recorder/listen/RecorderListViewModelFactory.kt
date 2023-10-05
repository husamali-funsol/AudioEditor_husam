package com.example.audioeditor.ui.fragments.features.recorder.listen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.audioeditor.repo.AppRepo

@Suppress("UNCHECKED_CAST")
class RecorderListViewModelFactory(private val appRepo: AppRepo): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecorderListViewModel::class.java)) {
            return RecorderListViewModel(appRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}