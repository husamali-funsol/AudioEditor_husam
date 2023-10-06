package com.example.audioeditor.ui.fragments.features.recorder.listen

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audioeditor.R
import com.example.audioeditor.databinding.BottomSheetDetailsBinding
import com.example.audioeditor.databinding.BottomSheetLibraryBinding
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.FragmentListenBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.repo.AppRepo
import com.example.audioeditor.ui.fragments.library.LibraryItemAdapter
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayList


class ListenFragment : Fragment() {

    private val binding by lazy{
        FragmentListenBinding.inflate(layoutInflater)
    }

    private lateinit var appRepo: AppRepo
    private lateinit var viewModel: RecorderListViewModel
    private lateinit var adapter: LibraryItemAdapter

    private var alertDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        context?.let{ appRepo = AppRepo(it) }
        val viewModelFactory = RecorderListViewModelFactory(appRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[RecorderListViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setViewsAndData()

    }

    //***************************************** Dialogs and Menu ***********************************************

    private fun showMenu(libItem: LibraryItemModel, position: Int, iv: ImageView){
        val popupMenu = PopupMenu(requireContext(), iv)

        popupMenu.menuInflater.inflate(R.menu.audio_item_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->

            if(menuItem.title == "Rename"){
                showRenameDialog(libItem, position)
            }
            else if (menuItem.title == "Details"){
                showDetailsBottomSheet(libItem, position)
            }
            else if (menuItem.title == "Delete"){
                showDeleteDialog(libItem, position)
            }
            true
        }
        // Showing the popup menu
        popupMenu.show()
    }

    private fun showRenameDialog(libItem: LibraryItemModel, position: Int) {
         val renameDialogBinding by lazy {
            RenameDialogBinding.inflate(layoutInflater)
        }
        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }
        val parent = renameDialogBinding.root.parent as? ViewGroup
        parent?.removeView(renameDialogBinding.root)
        val dialogView = renameDialogBinding.root
        alertDialogBuilder?.setView(dialogView)

        if (libItem != null) {
            // Set the initial text in your EditText (if needed)
            renameDialogBinding.etRenameRD.setText(libItem!!.title)
            renameDialogBinding.etRenameRD.setSelection(renameDialogBinding.etRenameRD.length())//placing cursor at the end of the text

        }

        renameDialogBinding.tvConfirmRD.setOnClickListener {
            // Handle the positive button click event here
            // You can retrieve the text entered in the EditText like this:
            val enteredText = renameDialogBinding.etRenameRD.text.toString()

            // Implement your logic here (e.g., renameFile(enteredText))
            val ext = libItem!!.extension
            Log.d("Debug", "entered text: $enteredText")
            Log.d("Debug", "ext: $ext")
            renameFile(enteredText, ext!!, libItem, position)

            alertDialog?.dismiss()
        }

        renameDialogBinding.tvCancelRD.setOnClickListener {
            // Handle the negative button click event here
            // This is where you can cancel the dialog if needed
            alertDialog?.dismiss()

        }

        alertDialog = alertDialogBuilder?.create()
        alertDialog!!.show()
    }

    private fun showDetailsBottomSheet(libItem: LibraryItemModel, position: Int) {
         val detailsBottomSheetDialogBinding by lazy {
            BottomSheetDetailsBinding.inflate(layoutInflater)
        }
        val detailsBottomSheet = BottomSheetDialog(requireContext())
        val parent = detailsBottomSheetDialogBinding.root.parent as? ViewGroup
        parent?.removeView(detailsBottomSheetDialogBinding.root)
        detailsBottomSheet.setContentView(detailsBottomSheetDialogBinding.root)

        if (libItem != null) {
            detailsBottomSheetDialogBinding.tvSetFilename.text = libItem!!.title
            detailsBottomSheetDialogBinding.tvSetTime.text = libItem!!.time
            detailsBottomSheetDialogBinding.tvSetPath.text = libItem!!.path
            detailsBottomSheetDialogBinding.tvSetSize.text = libItem!!.size
        }

        detailsBottomSheetDialogBinding.tvOkDetails.setOnClickListener {
            detailsBottomSheet.dismiss()
        }

        detailsBottomSheet.show()
    }

    private fun showDeleteDialog(libItem: LibraryItemModel, position: Int) {
         val deleteDialogBinding by lazy {
            DeleteDialogBinding.inflate(layoutInflater)
        }
        val alertDialogBuilder =
            context?.let{
                AlertDialog.Builder(it, R.style.CustomAlertDialogStyle)
            }
        val parent = deleteDialogBinding.root.parent as? ViewGroup
        parent?.removeView(deleteDialogBinding.root)
        val dialogView = deleteDialogBinding.root
        alertDialogBuilder?.setView(dialogView)


        deleteDialogBinding.tvDeleteBtnDD.setOnClickListener {
            // Handle the positive button click event here
            val filePath =
                libItem.path
            val originalFile = File(filePath!!)

            if (originalFile.exists()) {
                originalFile.delete()
                context?.scanFiles(originalFile)

            }

            context?.refreshMediaStoreForAudioFiles()
            adapter.itemRemoved(position)
            getList()
            alertDialog?.dismiss()
        }

        deleteDialogBinding.tvCancelDD.setOnClickListener {
            alertDialog?.dismiss()
        }

        alertDialog = alertDialogBuilder?.create()
        alertDialog!!.show()
    }

    //***************************************** Utility Functions ***********************************************
    private fun renameFile(newName: String, ext: String, libItem: LibraryItemModel, position: Int) {
        val filePath =
            libItem.path
        val newFileName = "$newName.$ext" // Provide the new file name
        val originalFile = File(filePath!!)
        // Create a File object for the new file with the desired name
        val directoryPath = originalFile.parentFile // Get the directory path
        val newFile = File(directoryPath, newFileName)

        // Rename the file
        if (originalFile.exists()) {
            if (originalFile.renameTo(newFile)) {

                val newPath = newFile.path
                val updatedFile = File(newPath!!)

                val currentTimeMillis = System.currentTimeMillis()
                newFile.setLastModified(currentTimeMillis)
                originalFile.setLastModified(currentTimeMillis)
                // Refresh the MediaStore to reflect the changes
                context?.refreshMediaStore(updatedFile)
                context?.showSmallLengthToast("Renaming Successful")
                adapter.itemUpdated(position, viewModel.getSingleAudioFile(position))

            } else {
                // Failed to rename the file
                // Handle the error accordingly
                context?.showSmallLengthToast("Renaming Failed")

            }
        } else {
            // The original file does not exist
            context?.showSmallLengthToast("Original File does not exist")

        }

        val newPath = newFile.path
        val updatedFile = File(newPath!!)
        context?.refreshMediaStore(updatedFile)

        context?.refreshMediaStoreForAudioFiles()
        getList()
    }

    private fun navigateToPlayer(libList: List<LibraryItemModel>, position: Int) {
        val libItemArray = libList.toTypedArray()
        val bundle = Bundle().apply {
//            putParcelableArray("AUDIO_ITEMS", libItemArray)
            putInt("AUDIO_POSITION", position)
            putParcelable("AUDIO_ITEM", libItemArray[position])
        }

        findNavController().apply {
            navigate(
                R.id.action_mainRecorderFragment_to_editAudio,
                bundle
            )
        }
    }

    private fun getList() {
        lifecycleScope.launch {
            viewModel.libraryList.collect { audioList ->
                withContext(Dispatchers.Main){
                    submitNewList(audioList)
                }
            }
        }
    }

    private fun submitNewList(audioList: ArrayList<LibraryItemModel>) {
        adapter.submitNewList(audioList)
    }

    private fun setViewsAndData(){

        context?.refreshMediaStoreForAudioFiles()

        context?.let{ binding.recyclerView.layoutManager = LinearLayoutManager(it) }
        adapter = LibraryItemAdapter(ArrayList() , object : LibraryItemAdapter.OnItemClicked{
            override fun onItemClicked(audioList: List<LibraryItemModel>, position: Int) {
                navigateToPlayer(audioList, position)
            }
            override fun onMenuClicked(audioItem: LibraryItemModel, position: Int, iv: ImageView) {
//                showOptions(audioItem, position)
                showMenu(audioItem, position, iv)
            }
        })

        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Perform your tasks on a background thread
            withContext(Dispatchers.Main) {
                binding.loader.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.ivNoAudio.visibility= View.GONE
                binding.tvNoAudio.visibility= View.GONE
            }
            // Fetch the list in the background
            getList()
            context?.refreshMediaStoreForAudioFiles()
            viewModel.getFiles()

            withContext(Dispatchers.Main) {
                binding.loader.visibility = View.GONE

                if (adapter.itemCount == 0) {
                    binding.loader.visibility = View.GONE
                    binding.ivNoAudio.visibility= View.VISIBLE
                    binding.tvNoAudio.visibility= View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.loader.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.ivNoAudio.visibility= View.GONE
                    binding.tvNoAudio.visibility= View.GONE
                }
            }

        }

    }

    //***************************************** Override Functions ***********************************************

    override fun onResume() {
        super.onResume()

        context?.refreshMediaStoreForAudioFiles()
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if(isVisibleToUser)
        {
//            getList()
//            context?.refreshMediaStoreForAudioFiles()
            CoroutineScope(Dispatchers.IO).launch{
                if(::viewModel.isInitialized){ viewModel.getFiles() }

            }
        }
    }


}