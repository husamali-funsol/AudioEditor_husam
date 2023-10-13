package com.example.audioeditor.ui.fragments

import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.TAG
import com.example.audioeditor.databinding.FragmentSavedScreenBinding
import com.example.audioeditor.utils.getFileNameFromUri
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.setOnOneClickListener
import java.io.File


class SavedScreenFragment : Fragment() {

    private val binding by lazy{
        FragmentSavedScreenBinding.inflate(layoutInflater)
    }

    private var audioUri: Uri? = null
    private var selected = ""
    private var audioPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var abc: String? = null


        abc= arguments?.getString("AUDIO_URI")
        audioUri = abc?.toUri()
        if(abc==null){
            audioUri = null
            abc = arguments?.getString("AUDIO_FILEPATH")
            audioPath = abc
        }

        val metadata = arguments?.getString("METADATA")
        binding.tvMetadata.text = metadata

        val name = abc?.let { File(it).name }
        binding.tvMusicTitle.text = name

        binding.btnBack.setOnOneClickListener {
            context?.performHapticFeedback()
            findNavController().apply {
                if(currentDestination?.id == R.id.savedScreenFragment){
                    popBackStack()
                }
            }
        }

        binding.btnDone.setOnOneClickListener {
            context?.performHapticFeedback()
            findNavController().apply {
                popBackStack(R.id.homeFragment, false)
//                navigate(R.id.homeFragment)
            }
        }

        binding.tvCallRingtone.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvCallRingtone)
            val x = setRingtone()
            Log.d(TAG, "onViewCreated: ringtone $x")
        }

        binding.tvContactsRingtone.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvContactsRingtone)
        }

        binding.tvAlarmSound.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvAlarmSound)
        }

        binding.tvNotificationSound.setOnOneClickListener {
            context?.performHapticFeedback()
            onTextViewClick(binding.tvNotificationSound)
        }

    }


    private fun onTextViewClick(clickedTextView: TextView) {
        // Reset all TextViews to white
        val allTextViews = listOf(
            binding.tvCallRingtone,
            binding.tvContactsRingtone,
            binding.tvNotificationSound,
            binding.tvAlarmSound
        )
        for (textView in allTextViews) {
//            textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            textView.setBackgroundResource(R.drawable.button_bg_light_blue_rounded)
            textView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.textColorBlack
                )
            )  // Set your desired text color here
        }

        selected = clickedTextView.text.toString()
        Log.d(TAG, "onTextViewClick: $selected")

        // Change the background color of the clicked TextView to blue
        clickedTextView.setBackgroundResource(R.drawable.button_bg_rounded)
        clickedTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.white
            )
        )  // Set your desired text color here

    }

    private fun setRingtone(): Boolean{
        return try{
            Log.d(TAG, "Setting ringtone. Audio URI: $audioUri, Audio Path: $audioPath")

            if (audioUri != null) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    requireContext(),
                    RingtoneManager.TYPE_RINGTONE,
                    audioUri
                )
                true
            } else {
                RingtoneManager.setActualDefaultRingtoneUri(
                    requireContext(),
                    RingtoneManager.TYPE_RINGTONE,
                    Uri.fromFile(File(audioPath!!))
                )
                true
            }
        }
        catch(e: Exception){
            Log.e(TAG, "Error setting ringtone: ${e.message}")
            e.printStackTrace()
            false
        }
    }

}