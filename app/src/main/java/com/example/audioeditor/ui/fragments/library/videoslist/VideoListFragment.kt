package com.example.audioeditor.ui.fragments.library.videoslist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.audioeditor.R
import com.example.audioeditor.databinding.BottomSheetDetailsBinding
import com.example.audioeditor.databinding.BottomSheetLibraryBinding
import com.example.audioeditor.databinding.DeleteDialogBinding
import com.example.audioeditor.databinding.FragmentVideoListBinding
import com.example.audioeditor.databinding.RenameDialogBinding
import com.example.audioeditor.models.LibraryItemModel
import com.example.audioeditor.repo.AppRepo
import com.example.audioeditor.ui.fragments.library.LibraryItemAdapter
import com.example.audioeditor.utils.refreshMediaStore
import com.example.audioeditor.utils.refreshMediaStoreForAudioFiles
import com.example.audioeditor.utils.scanFiles
import com.example.audioeditor.utils.showSmallLengthToast
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayList


class VideoListFragment : Fragment() {

    private var alertDialog: AlertDialog? = null

    private val libraryBottomSheetDialogBinding by lazy {
        BottomSheetLibraryBinding.inflate(layoutInflater)
    }
    private val detailsBottomSheetDialogBinding by lazy {
        BottomSheetDetailsBinding.inflate(layoutInflater)
    }
    private val renameDialogBinding by lazy {
        RenameDialogBinding.inflate(layoutInflater)
    }
    private val deleteDialogBinding by lazy {
        DeleteDialogBinding.inflate(layoutInflater)
    }

    private lateinit var audioUri: Uri

    private lateinit var appRepo: AppRepo
    private lateinit var viewModel: VideoListViewModel
    private lateinit var adapter: LibraryItemAdapter

    private val binding by lazy {
        FragmentVideoListBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        context?.let{ appRepo = AppRepo(it) }
        val viewModelFactory = VideoListViewModelFactory(appRepo)
        viewModel = ViewModelProvider(this, viewModelFactory)[VideoListViewModel::class.java]

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setViewsAndData()

    }





    //*****************************************  Dialogs  ***********************************************

    private fun showRenameDialog(
        libItem: LibraryItemModel,
        position: Int,
        bottomSheet: BottomSheetDialog
    ) {
        val alertDialogBuilder =
            context?.let {
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
            renameDialogBinding.etRenameRD.setSelectAllOnFocus(true)
            renameDialogBinding.etRenameRD.highlightColor = resources.getColor(R.color.thirtyPrecentAppBlue)

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
        bottomSheet.dismiss()
        alertDialog!!.show()
    }

    private fun showDeleteDialog(
        libItem: LibraryItemModel,
        position: Int,
        bottomSheet: BottomSheetDialog
    ) {
        val alertDialogBuilder =
            context?.let {
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
        bottomSheet.dismiss()
        alertDialog!!.show()
    }

    //*****************************************  Bottom Sheet  ***********************************************

    private fun openShareBottomSheet(libItem: LibraryItemModel) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"

        audioUri = libItem.uri!!

        shareIntent.putExtra(Intent.EXTRA_STREAM, audioUri)

        startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }

    private fun showDetailsBottomSheet(
        libItem: LibraryItemModel,
        bottomSheet: BottomSheetDialog
    ) {
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
        bottomSheet.dismiss()

        detailsBottomSheetDialogBinding.tvOkDetails.setOnClickListener {
            detailsBottomSheet.dismiss()
        }

        detailsBottomSheet.show()
    }

    //***************************************** Utility Functions  ***********************************************

    private fun setViewsAndData() {
        context?.refreshMediaStoreForAudioFiles()
//        listAudioFiles()

        context?.let{ binding.rvMyVideo.layoutManager = LinearLayoutManager(it) }
        adapter = LibraryItemAdapter(ArrayList() , object :LibraryItemAdapter.OnItemClicked{
            override fun onItemClicked(audioList: List<LibraryItemModel>, position: Int) {
                navigateToPlayer(audioList, position)
            }
            override fun onMenuClicked(audioItem: LibraryItemModel, position: Int, iv: ImageView) {
                showOptions(audioItem, position)
            }
        })

        binding.rvMyVideo.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Perform your tasks on a background thread
            withContext(Dispatchers.Main) {
                binding.loader.visibility = View.VISIBLE
                binding.rvMyVideo.visibility = View.GONE
                binding.ivNoVideo.visibility= View.GONE
                binding.tvNoVideo.visibility= View.GONE
            }
            // Fetch the list in the background
            getList()
            context?.refreshMediaStoreForAudioFiles()
            viewModel.getFiles()

            withContext(Dispatchers.Main) {

                binding.loader.visibility = View.GONE

                if (adapter.itemCount == 0) {
                    binding.loader.visibility = View.GONE
                    binding.ivNoVideo.visibility= View.VISIBLE
                    binding.tvNoVideo.visibility= View.VISIBLE
                    binding.rvMyVideo.visibility = View.GONE
                } else {
                    binding.loader.visibility = View.GONE
                    binding.rvMyVideo.visibility = View.VISIBLE
                    binding.ivNoVideo.visibility= View.GONE
                    binding.tvNoVideo.visibility= View.GONE
                }
            }
        }
    }

    private fun getList() {
        lifecycleScope.launch {
            viewModel.libraryList.collect { libraryList ->
                withContext(Dispatchers.Main){
                    submitNewList(libraryList)
                }
            }
        }
    }

    private fun submitNewList(libraryList: ArrayList<LibraryItemModel>) {
        adapter.submitNewList(libraryList)
    }

    private fun navigateToPlayer(libList: List<LibraryItemModel>, position: Int) {
        val libItemArray = libList.toTypedArray()
        val bundle = Bundle().apply {
            putParcelableArray("VIDEO_ITEMS", libItemArray)
            putInt("VIDEO_POSITION", position)
        }

        findNavController().apply {
            navigate(
                R.id.action_libraryFragment_to_libraryVideoPlayerFragment,
                bundle
            )

        }
//        callBack.invoke(libList, position)
    }

    private fun showOptions(libItem: LibraryItemModel, position: Int) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val parent = libraryBottomSheetDialogBinding.root.parent as? ViewGroup
        parent?.removeView(libraryBottomSheetDialogBinding.root)
        bottomSheet.setContentView(libraryBottomSheetDialogBinding.root)

        if (libItem != null) {
            libraryBottomSheetDialogBinding.tvTitleLibSheet.text = libItem!!.title
            libraryBottomSheetDialogBinding.tvMetaDataLibSheet.text = libItem!!.metadata
        }

        libraryBottomSheetDialogBinding.tvRenameLibSheet.setOnClickListener {
//                renameDialogBinding = RenameDialogBinding.inflate(layoutInflater)
            showRenameDialog(libItem, position, bottomSheet)
        }

        libraryBottomSheetDialogBinding.tvDetailLibSheet.setOnClickListener {
            showDetailsBottomSheet(libItem, bottomSheet)

        }

        libraryBottomSheetDialogBinding.tvShareLibSheet.setOnClickListener {

            openShareBottomSheet(libItem)
        }

        libraryBottomSheetDialogBinding.tvDeleteLibSheet.setOnClickListener {
//                deleteDialogBinding = DeleteDialogBinding.inflate(layoutInflater)
            showDeleteDialog(libItem, position, bottomSheet)

        }
        bottomSheet.show()
    }

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
                adapter.itemUpdated(position, viewModel.getSingleVideoFile(position))

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




}