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
import com.example.musicalquiz.model.Track

/**
 * Adapter for displaying tracks in a RecyclerView.
 * Handles the display of track information including cover image, title, and artist.
 */
class TrackAdapter : ListAdapter<Track, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    private var onItemClickListener: ((Track) -> Unit)? = null
    private var onItemLongClickListener: ((Track) -> Unit)? = null

    /**
     * Sets a click listener for track items.
     * @param listener Callback function to be invoked when a track is clicked
     */
    fun setOnItemClickListener(listener: (Track) -> Unit) {
        onItemClickListener = listener
    }

    /**
     * Sets a long click listener for track items.
     * @param listener Callback function to be invoked when a track is long clicked
     */
    fun setOnItemLongClickListener(listener: (Track) -> Unit) {
        onItemLongClickListener = listener
    }

    /**
     * ViewHolder class for track items.
     * Holds references to the views that display track information.
     */
    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeLabel: TextView = itemView.findViewById(R.id.typeLabel)
        private val image: ImageView = itemView.findViewById(R.id.coverImageView)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val artist: TextView = itemView.findViewById(R.id.artist)

        fun bind(track: Track) {
            typeLabel.text = "Track"
            typeLabel.setBackgroundResource(R.drawable.bg_track_label)
            title.text = track.title
            subtitle.text = track.album.title
            artist.text = track.artist.name
            
            Glide.with(itemView.context)
                .load(track.album.cover)
                .into(image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
        holder.itemView.setOnClickListener { onItemClickListener?.invoke(track) }
        holder.itemView.setOnLongClickListener { 
            onItemLongClickListener?.invoke(track)
            true
        }
    }
}

/**
 * Callback class for calculating differences between old and new track lists.
 * Used by DiffUtil to efficiently update the RecyclerView.
 */
class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }
} 