package com.example.audioeditor.ui.fragments.features.audiocompress

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentAudioCompressBinding


class AudioCompress : Fragment() {

    private lateinit var binding: FragmentAudioCompressBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAudioCompressBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

}