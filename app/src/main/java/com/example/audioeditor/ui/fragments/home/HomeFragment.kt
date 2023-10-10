package com.example.audioeditor.ui.fragments.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentHomeBinding
import com.example.audioeditor.utils.setOnOneClickListener


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConvert.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_convertFormat)
        }

        binding.btnCompress.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_audioCompress)
        }

        binding.btnSpeed.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_audioSpeed)
        }

        binding.btnTrim.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trimAudio)
        }

        binding.btnRecorder.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_mainRecorderFragment)
        }

        binding.btnVideotoAudio.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_videoToAudioFragment)
        }

        binding.btnTexttoAudio.setOnOneClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_textToAudioFragment)
        }


    }

}