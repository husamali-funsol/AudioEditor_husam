package com.example.audioeditor.ui.fragments.library

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.audioeditor.R

class LibraryItemAdapter(
    private var mList: ArrayList<LibraryItemModel>,
    private val showOptions: (LibraryItemModel , Int) -> Unit,
    private val navigateToPlayer: (List<LibraryItemModel>, Int) -> Unit,
) :
    RecyclerView.Adapter<LibraryItemAdapter.ViewHolder>() {

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle_libItem)
        val menu: ImageView = itemView.findViewById(R.id.ibMenu_libItem)
        val metadata: TextView = itemView.findViewById(R.id.tvMetaData_libItem)
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar_libItem)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LibraryItemAdapter.ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.library_item, parent, false)

        return ViewHolder(view)

    }

    fun submitNewList(newList: ArrayList<LibraryItemModel>) {

        mList = newList
        notifyDataSetChanged()
    }

    fun itemUpdated(position: Int, newItem : LibraryItemModel){
        mList[position] = newItem
        notifyItemChanged(position)
    }

    override fun onBindViewHolder(holder: LibraryItemAdapter.ViewHolder, position: Int) {
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
            showOptions(audioItem, position)

        }

        holder.title.setOnClickListener {
//            playerActivity(audioItem)
            navigateToPlayer(mList, position)
        }

        holder.metadata.setOnClickListener {
//            playerActivity(audioItem)
            navigateToPlayer(mList, position)
        }

        holder.avatar.setOnClickListener {
//            playerActivity(audioItem)
            navigateToPlayer(mList, position)
        }


    }

    override fun getItemCount(): Int {
        return mList.size
    }
}