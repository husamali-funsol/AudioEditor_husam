package com.example.audioeditor.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.audioeditor.R
import com.example.audioeditor.models.AudioItemModel


class AudioItemAdapter(private val mList: List<AudioItemModel>) :
    RecyclerView.Adapter<AudioItemAdapter.ViewHolder>() {

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val filename: TextView = itemView.findViewById(R.id.tvFilename)
        val btnRemove: ImageView = itemView.findViewById(R.id.btnRemove)
        val dragHandle: ImageView = itemView.findViewById(R.id.dragHandle)
        val albumImage: ImageView = itemView.findViewById(R.id.dragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.audio_tile, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audioItem = mList[position]
//        holder.textView.text = audioItem.filename

        holder.filename.text = audioItem.filename

        when(position){
            0 -> {
                firstItem(holder)
            }
            1 -> {
                secondItem(holder)
            }
            2->{
                thirdItem(holder)
            }
        }
    }

    private fun thirdItem(holder: ViewHolder) {
        holder.dragHandle.setImageResource(R.drawable.drag_handle_purple)
        holder.btnRemove.setImageResource(R.drawable.dont_disturb_purple)
        holder.filename.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.purple))
    }

    private fun secondItem(holder: ViewHolder) {
        holder.dragHandle.setImageResource(R.drawable.drag_handle_yellow)
        holder.btnRemove.setImageResource(R.drawable.dont_disturb_yellow)
        holder.filename.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.yellow))
    }

    private fun firstItem(holder: ViewHolder) {
        holder.dragHandle.setImageResource(R.drawable.drag_handle_maroon)
        holder.btnRemove.setImageResource(R.drawable.dont_disturb_maroon)
        holder.filename.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.maroon))
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}