package com.example.audioeditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.audioeditor.databinding.ActivityMainBinding
import com.example.audioeditor.databinding.DiscardDialogBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.repo.AppRepo
import com.example.audioeditor.ui.fragments.library.audioslist.AudioListViewModel
import com.example.audioeditor.ui.fragments.library.audioslist.AudioListViewModelFactory
import com.example.audioeditor.utils.performHapticFeedback

var TAG = "AudioEditor"

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    private lateinit var appRepo: AppRepo
    private lateinit var viewModel: AudioListViewModel

    private var quitAlertDialog: AlertDialog? = null
    private val quitDialogBinding by lazy{
        QuitDialogBinding.inflate(layoutInflater)
    }
    private var quitDialogView: ConstraintLayout? = null

    private var fragmnetsList = listOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmnetsList = listOf(
            R.id.trimAudio,
            R.id.convertFormat,
            R.id.audioCompress,
            R.id.audioSpeed,
            R.id.mainRecorderFragment
        )


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

    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        val navController = findNavController(R.id.navigation_fragment)

        if(navController.currentDestination?.id == R.id.mainRecorderFragment){
            showDiscardDialog()
        }
        else if(navController.currentDestination?.id in fragmnetsList){
            showQuitDialog()
        }
        else{
            super.onBackPressed()
        }


    }

    private fun showQuitDialog() {
        val alertDialogBuilder =
            this.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = quitDialogBinding.root
        alertDialogBuilder?.setView(dialogView)
        quitAlertDialog = alertDialogBuilder?.create()

        quitDialogView = dialogView


        quitDialogBinding.tvNo.setOnClickListener {
            this.performHapticFeedback()
            com.example.audioeditor.utils.dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitDialogBinding.tvYes.setOnClickListener {
            this.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController(R.id.navigation_fragment).apply{
                if(currentDestination?.id in fragmnetsList){
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            com.example.audioeditor.utils.dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog?.setOnDismissListener {
            com.example.audioeditor.utils.dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog!!.show()
    }

    private fun showDiscardDialog(){

        var discardAlertDialog: AlertDialog? = null
        val discardDialogBinding by lazy {
            DiscardDialogBinding.inflate(layoutInflater)
        }

        val alertDialogBuilder =
                AlertDialog.Builder(this, R.style.CustomAlertDialogStyle)


        val dialogView = discardDialogBinding.root
        alertDialogBuilder.setView(dialogView)
        discardAlertDialog = alertDialogBuilder.create()

        val quitDialogView = dialogView


        discardDialogBinding.tvCancel.setOnClickListener {
            this.performHapticFeedback()
            com.example.audioeditor.utils.dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardDialogBinding.tvDiscard.setOnClickListener {
            this.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController(R.id.navigation_fragment).apply {
                if (currentDestination?.id == R.id.mainRecorderFragment) {
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            com.example.audioeditor.utils.dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardAlertDialog.setOnDismissListener {
            com.example.audioeditor.utils.dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardAlertDialog.show()
    }


}