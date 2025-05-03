package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.model.Album

/**
 * Adapter for displaying albums in a RecyclerView.
 * Handles the display of album information including cover image, title, and artist.
 */
class AlbumAdapter : ListAdapter<Album, AlbumAdapter.AlbumViewHolder>(AlbumDiffCallback()) {

    private var onItemClickListener: ((Album) -> Unit)? = null

    /**
     * Sets a click listener for album items.
     * @param listener Callback function to be invoked when an album is clicked
     */
    fun setOnItemClickListener(listener: (Album) -> Unit) {
        onItemClickListener = listener
    }

    /**
     * ViewHolder class for album items.
     * Holds references to the views that display album information.
     */
    class AlbumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeLabel: TextView = itemView.findViewById(R.id.typeLabel)
        private val image: ImageView = itemView.findViewById(R.id.coverImageView)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val artist: TextView = itemView.findViewById(R.id.artist)

        fun bind(album: Album) {
            typeLabel.text = "Album"
            typeLabel.setBackgroundResource(R.drawable.bg_album_label)
            title.text = album.title
            artist.text = album.artist.name
            subtitle.text = album.releaseDate?.substring(0, 4) ?: ""
            
            Glide.with(itemView.context)
                .load(album.cover)
                .into(image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = getItem(position)
        holder.bind(album)
        holder.itemView.setOnClickListener { onItemClickListener?.invoke(album) }
    }
}

/**
 * Callback class for calculating differences between old and new album lists.
 * Used by DiffUtil to efficiently update the RecyclerView.
 */
class AlbumDiffCallback : DiffUtil.ItemCallback<Album>() {
    override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem == newItem
    }
} 