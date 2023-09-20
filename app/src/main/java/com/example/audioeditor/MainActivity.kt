package com.example.audioeditor

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.audioeditor.databinding.ActivityMainBinding
import com.example.audioeditor.repo.AppRepo
import com.example.audioeditor.ui.fragments.library.audioslist.AudioListViewModel
import com.example.audioeditor.ui.fragments.library.audioslist.AudioListViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

var TAG = "AudioEditor"

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private lateinit var appRepo: AppRepo
    private lateinit var viewModel: AudioListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        appRepo = AppRepo(this)
        val viewModelFactory = AudioListViewModelFactory(appRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[AudioListViewModel::class.java]


        // Bottom Navigation Bar & Navigation Setup
        val bottomNavView = binding.bottomNavigationView
//            findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        val navHost = findNavController(R.id.navigation_fragment)

        bottomNavView.setupWithNavController(navHost)
        bottomNavView.itemIconTintList = null

        // Bottom Navigation Bar Visibility Checks
        navHost.addOnDestinationChangedListener{ controller , destination , arguments,->

            if((destination.id == R.id.homeFragment) || (destination.id == R.id.libraryFragment) || (destination.id == R.id.settingsFragment) ){
binding.bottomNavigationView.visibility = View.VISIBLE
            }
            else{
                binding.bottomNavigationView.visibility = View.GONE
            }
        }


//        navcontroller.addOnDestinationChangedListener { controller, destination, arguments ->
//            if ((destination.id == R.id.homelandingfrag) || (destination.id == R.id.tools_main_frag2) || (destination.id == R.id.speedtesthome)) {
//                binding.bottomNav.visibility = View.VISIBLE
//                println("yes Visible")
//                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//                supportActionBar?.hide()
//            } else {
//                println("yes Gone")
//                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//                supportActionBar?.hide()
//                binding.bottomNav.visibility = View.GONE
//            }
//        }

    }
}