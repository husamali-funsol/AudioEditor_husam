package com.example.audioeditor.ui.fragments.library.videoslist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentMyVideoBinding


class MyVideoFragment : Fragment() {

    private val binding by lazy {
        FragmentMyVideoBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

}