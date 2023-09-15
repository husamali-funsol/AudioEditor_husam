package com.example.audioeditor.ui.fragments.splash

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.MainActivity
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentStartBinding
import java.util.Timer
import java.util.TimerTask

class StartFragment : Fragment() {

    lateinit var binding : FragmentStartBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_start, container, false)
        binding = FragmentStartBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                findNavController().navigate(R.id.action_startFragment_to_onBoardingFragment)
                }
            }
        }, 1000)
    }

}