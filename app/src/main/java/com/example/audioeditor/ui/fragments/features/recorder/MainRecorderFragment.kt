package com.example.audioeditor.ui.fragments.features.recorder

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import com.example.audioeditor.R
import com.example.audioeditor.databinding.DiscardDialogBinding
import com.example.audioeditor.databinding.FragmentMainRecorderBinding
import com.example.audioeditor.databinding.QuitDialogBinding
import com.example.audioeditor.ui.fragments.features.recorder.listen.ListenFragment
import com.example.audioeditor.ui.fragments.features.recorder.recorder.RecorderFragment
import com.example.audioeditor.ui.fragments.library.ViewPagerAdapter
import com.example.audioeditor.utils.dismissDialog
import com.example.audioeditor.utils.performHapticFeedback
import com.example.audioeditor.utils.setOnOneClickListener


class MainRecorderFragment : Fragment() {

    private val binding by lazy{
        FragmentMainRecorderBinding.inflate(layoutInflater)
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

        var index = 0
        val temp = arguments?.getInt("VIEWPAGER_INDEX")
        if(temp!= null){
            index = temp
        }

        val adapter = ViewPagerAdapter(childFragmentManager)

        adapter.addFragment(RecorderFragment(), "Recorder")
        adapter.addFragment(ListenFragment(), "Listen")

        binding.viewPager.adapter = adapter
        binding.tabs.setupWithViewPager(binding.viewPager)
        binding.viewPager.currentItem = index

        binding.btnBack.setOnOneClickListener{

//            showDiscardDialog()

            showQuitDialog()

        }

    }

    private fun showDiscardDialog(){

        var discardAlertDialog: AlertDialog? = null
        val discardDialogBinding by lazy {
            DiscardDialogBinding.inflate(layoutInflater)
        }

        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }

        val dialogView = discardDialogBinding.root
        alertDialogBuilder?.setView(dialogView)
        discardAlertDialog = alertDialogBuilder?.create()

        val quitDialogView = dialogView


        discardDialogBinding.tvCancel.setOnClickListener {
            context?.performHapticFeedback()
            dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardDialogBinding.tvDiscard.setOnClickListener {
            context?.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController().apply {
                if (currentDestination?.id == R.id.mainRecorderFragment) {
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardAlertDialog?.setOnDismissListener {
            dismissDialog(discardAlertDialog, quitDialogView)
        }

        discardAlertDialog!!.show()
    }

    private fun showQuitDialog() {

        var quitAlertDialog: AlertDialog? = null
        val quitDialogBinding by lazy {
            QuitDialogBinding.inflate(layoutInflater)
        }
        var quitDialogView: ConstraintLayout? = null

        val alertDialogBuilder =
            context?.let {
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }


        val dialogView = quitDialogBinding.root
        alertDialogBuilder?.setView(dialogView)
        quitAlertDialog = alertDialogBuilder?.create()

        quitDialogView = dialogView

            quitDialogBinding.tvText.text = "Are you sure you want to quit?"


        quitDialogBinding.tvNo.setOnClickListener {
            context?.performHapticFeedback()
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitDialogBinding.tvYes.setOnClickListener {
            context?.performHapticFeedback()
            // Clear the back stack and navigate to the home fragment
            findNavController().apply {
                if (currentDestination?.id == R.id.mainRecorderFragment) {
                    popBackStack()
                    navigate(R.id.homeFragment)
                }
            }
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog?.setOnDismissListener {
            dismissDialog(quitAlertDialog, quitDialogView)
        }

        quitAlertDialog!!.show()
    }


}