package com.example.audioeditor.ui.fragments.features

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.FragmentAudioSpeedBinding
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.setOnOneClickListener

class AudioSpeed : Fragment() {

    private val binding by lazy {
        FragmentAudioSpeedBinding.inflate(layoutInflater)
    }

    private var selectedOption = "0.5"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tv05x.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv05x)
        }

        binding.tv075x.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv075x)
        }

        binding.tv10x.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv10x)
        }

        binding.tv125x.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv125x)
        }

        binding.tv15x.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv15x)
        }

        binding.tv20x.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tv20x)
        }
    }


    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(
            binding.tv05x,
            binding.tv075x,
            binding.tv10x,
            binding.tv125x,
            binding.tv15x,
            binding.tv20x
        )
        for (textView in allTextViews) {

            context?.let{
                textView.setBackgroundResource(R.drawable.button_bg_white)
                textView.setTextColor(
                    ContextCompat.getColor(
                        it,
                        R.color.textColorDarkGrey
                    )
                )
            }
        }

        val text = clickedTextView.text.toString()
        selectedOption = text.removeSuffix("x")
        Log.d(TAG, "onTextViewClick: $selectedOption")
        // Change the background color of the clicked TextView to blue
        clickedTextView.setBackgroundResource(R.drawable.button_bg)
        context?.let{
            clickedTextView.setTextColor(
                ContextCompat.getColor(
                    it,
                    R.color.white
                )
            )
        }
    }
}