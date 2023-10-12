package com.example.audioeditor.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.audioeditor.R
import com.example.audioeditor.models.LibraryItemModel

class LibraryItemAdapter(
    private var mList: ArrayList<LibraryItemModel>,
    private val onItemClicked: OnItemClicked
) :
    RecyclerView.Adapter<LibraryItemAdapter.ViewHolder>() {
    interface OnItemClicked {
        fun onItemClicked(audioList: List<LibraryItemModel>, position: Int)

        fun onMenuClicked(audioItem: LibraryItemModel, position: Int, iv: ImageView)
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle_libItem)
        val menu: ImageView = itemView.findViewById(R.id.ibMenu_libItem)
        val metadata: TextView = itemView.findViewById(R.id.tvMetaData_libItem)
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar_libItem)
        val view: View = itemView.findViewById(R.id.view)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.library_item, parent, false)

        return ViewHolder(view)

    }

    fun submitNewList(newList: ArrayList<LibraryItemModel>) {
        mList = newList
        notifyDataSetChanged()
    }

    fun itemUpdated(position: Int, newItem: LibraryItemModel) {
        mList[position] = newItem
        notifyItemChanged(position)
    }

    fun itemRemoved(position: Int) {
        mList.removeAt(position)  // Remove the item at the given position
        notifyItemRemoved(position)  // Notify the adapter about the item removal

        // Update indices for items after the removed item
        for (i in position until mList.size) {
            notifyItemChanged(i)  // Notify the adapter about data change for updated positions
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioItem = mList[position]
        holder.title.text = audioItem.title
        holder.metadata.text = audioItem.metadata

        Log.d("Debug", "inside on bind ")


        Glide.with(holder.itemView.context)
            .load(audioItem.albumArt) // This can handle null albumArt
            .placeholder(R.drawable.placeholder_image) // Placeholder image
            .error(R.drawable.placeholder_image) // Error image
            .into(holder.avatar)

        holder.menu.setOnClickListener {
            onItemClicked.onMenuClicked(audioItem, position, holder.menu)
        }

//        holder.title.setOnClickListener {
//            onItemClicked.onItemClicked(mList, position)
//        }
//
//        holder.metadata.setOnClickListener {
//            onItemClicked.onItemClicked(mList, position)
//        }
//
//        holder.avatar.setOnClickListener {
//            onItemClicked.onItemClicked(mList, position)
//        }

        holder.view.setOnClickListener {
            onItemClicked.onItemClicked(mList, position)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}