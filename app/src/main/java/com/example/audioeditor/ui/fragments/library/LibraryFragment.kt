package com.example.audioeditor.ui.fragments.library

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.databinding.FragmentLibraryBinding
import com.example.audioeditor.ui.fragments.library.audioslist.MyAudioFragment
import com.example.audioeditor.ui.fragments.library.videoslist.MyVideoFragment


class LibraryFragment : Fragment() {

    private lateinit var binding: FragmentLibraryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLibraryBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    private val navigateToFrag = fun(libItem: List<LibraryItemModel>, position: Int) {
        val libItemArray = libItem.toTypedArray() // Convert list to array

        val bundle = Bundle().apply {
            putParcelableArray("AUDIO_ITEMS", libItemArray)
            putInt("AUDIO_POSITION", position)
        }
        findNavController().navigate(R.id.action_libraryFragment_to_myAudioPlayerFragment2, bundle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ViewPagerAdapter(childFragmentManager)

        adapter.addFragment(MyAudioFragment(navigateToFrag), "My Audio")
        adapter.addFragment(MyVideoFragment(), "My Video")

        binding.viewPager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.viewPager)

    }
}