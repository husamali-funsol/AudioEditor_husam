package com.example.audioeditor.ui.fragments.library

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.audioeditor.adapters.ViewPagerAdapter
import com.example.audioeditor.databinding.FragmentLibraryBinding
import com.example.audioeditor.ui.fragments.library.audioslist.AudioListFragment
import com.example.audioeditor.ui.fragments.library.videoslist.VideoListFragment


class LibraryFragment : Fragment() {

    private val binding: FragmentLibraryBinding by lazy {
        FragmentLibraryBinding.inflate(layoutInflater)
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

        val adapter = ViewPagerAdapter(childFragmentManager)

        adapter.addFragment(AudioListFragment(), "My Audio")
        adapter.addFragment(VideoListFragment(), "My Video")

        binding.viewPager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.viewPager)

    }
}