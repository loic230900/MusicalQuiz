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
 * This adapter:
 * - Displays track information in a card format
 * - Shows track cover image, title, album, and artist
 * - Handles click and long-click events
 * - Uses Glide for efficient image loading
 * - Implements DiffUtil for efficient list updates
 * 
 * The adapter uses a ViewHolder pattern to optimize view recycling
 * and provides callbacks for user interactions.
 */
class TrackAdapter : ListAdapter<Track, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    private var onItemClickListener: ((Track) -> Unit)? = null
    private var onItemLongClickListener: ((Track) -> Unit)? = null

    /**
     * Sets a click listener for track items.
     * The listener is invoked when a user taps on a track item.
     * 
     * @param listener Callback function that receives the clicked Track
     */
    fun setOnItemClickListener(listener: (Track) -> Unit) {
        onItemClickListener = listener
    }

    /**
     * Sets a long-click listener for track items.
     * The listener is invoked when a user long-presses a track item.
     * 
     * @param listener Callback function that receives the long-clicked Track
     */
    fun setOnItemLongClickListener(listener: (Track) -> Unit) {
        onItemLongClickListener = listener
    }

    /**
     * ViewHolder class for track items.
     * Holds references to the views that display track information:
     * - Type label (Track)
     * - Cover image
     * - Title
     * - Album subtitle
     * - Artist name
     */
    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeLabel: TextView = itemView.findViewById(R.id.typeLabel)
        private val image: ImageView = itemView.findViewById(R.id.coverImageView)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val artist: TextView = itemView.findViewById(R.id.artist)

        /**
         * Binds track data to the ViewHolder's views.
         * Sets:
         * - Track type label with appropriate background
         * - Track title
         * - Album subtitle
         * - Artist name
         * - Album cover image using Glide
         * 
         * @param track The Track object containing the data to display
         */
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

    /**
     * Creates a new ViewHolder instance.
     * Inflates the track_item layout for the ViewHolder.
     * 
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new TrackViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view)
    }

    /**
     * Binds the track data at the specified position to the ViewHolder.
     * Sets up click and long-click listeners for the item.
     * 
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the data set
     */
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
 * Used by DiffUtil to efficiently update the RecyclerView by:
 * - Identifying which items have changed
 * - Minimizing the number of view updates
 * - Animating changes smoothly
 */
class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
    /**
     * Checks if two items represent the same track.
     * Uses track ID for comparison.
     * 
     * @param oldItem The old track item
     * @param newItem The new track item
     * @return true if the items represent the same track
     */
    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.id == newItem.id
    }

    /**
     * Checks if two items have the same content.
     * Compares all track properties.
     * 
     * @param oldItem The old track item
     * @param newItem The new track item
     * @return true if the items have the same content
     */
    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem == newItem
    }
} 