package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.model.Album

class AlbumAdapter(private var items: List<Album>) :
    RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeLabel: TextView = itemView.findViewById(R.id.typeLabel)
        val image: ImageView = itemView.findViewById(R.id.coverImageView)
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val artist: TextView = itemView.findViewById(R.id.artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = items[position]
        holder.typeLabel.setText(R.string.album)
        holder.typeLabel.setBackgroundResource(R.drawable.bg_album_label)
        holder.title.text = album.title
        holder.subtitle.text = "" // No subtitle for albums
        holder.artist.text = album.artist.name
        Glide.with(holder.itemView).load(album.cover).into(holder.image)
    }

    fun updateData(newItems: List<Album>) {
        val diffCallback = AlbumDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}

class AlbumDiffCallback(
    private val oldList: List<Album>,
    private val newList: List<Album>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
} 