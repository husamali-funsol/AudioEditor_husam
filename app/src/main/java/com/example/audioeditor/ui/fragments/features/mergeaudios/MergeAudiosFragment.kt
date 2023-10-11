package com.example.audioeditor.ui.fragments.features.mergeaudios

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentMergeAudiosBinding


class MergeAudiosFragment : Fragment() {

    private val binding by lazy {
        FragmentMergeAudiosBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

}