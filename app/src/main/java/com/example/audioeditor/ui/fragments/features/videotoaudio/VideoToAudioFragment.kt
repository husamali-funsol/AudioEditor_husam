package com.example.audioeditor.ui.fragments.features.videotoaudio

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentVideoToAudioBinding


class VideoToAudioFragment : Fragment() {

    private val binding by lazy {
        FragmentVideoToAudioBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}