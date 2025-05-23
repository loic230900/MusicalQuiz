package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicalquiz.R
import com.example.musicalquiz.model.Track

/**
 * Adapter for displaying a list of tracks in a RecyclerView.
 * Provides functionality for track preview playback and navigation to track details.
 * @param onTrackClick Callback function to be invoked when a track is clicked
 * @param onPreviewClick Callback function to be invoked when a track's preview button is clicked
 * @param onDeleteClick Callback function to be invoked when a track's delete button is clicked
 * @param showDeleteButton Whether to show the delete button (default: true)
 */
class TrackListAdapter(
    private val onTrackClick: (Track) -> Unit,
    private val onPreviewClick: (Track) -> Unit,
    private val onDeleteClick: (Track) -> Unit,
    private val showDeleteButton: Boolean = true
) : ListAdapter<Track, TrackListAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    /**
     * ViewHolder class for track list items.
     * Manages the display of track information and handles user interactions.
     */
    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val trackNumberText: TextView = itemView.findViewById(R.id.trackNumberText)
        private val trackTitleText: TextView = itemView.findViewById(R.id.trackTitleText)
        private val trackArtistText: TextView = itemView.findViewById(R.id.trackArtistText)
        private val trackDurationText: TextView = itemView.findViewById(R.id.trackDurationText)
        private val previewButton: ImageButton = itemView.findViewById(R.id.previewButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        /**
         * Binds track data to the ViewHolder's views.
         * @param track The track to display
         */
        fun bind(track: Track) {
            trackNumberText.text = itemView.context.getString(R.string.track_number, adapterPosition + 1)
            trackTitleText.text = track.title
            trackArtistText.text = track.artist.name
            trackDurationText.text = formatDuration(track.duration)

            // Set click listeners
            itemView.setOnClickListener { onTrackClick(track) }
            previewButton.setOnClickListener { onPreviewClick(track) }
            deleteButton.setOnClickListener { onDeleteClick(track) }

            // Show/hide preview button based on preview availability
            previewButton.visibility = if (track.preview != null) View.VISIBLE else View.GONE
            
            // Show/hide delete button based on showDeleteButton parameter
            deleteButton.visibility = if (showDeleteButton) View.VISIBLE else View.GONE
        }

        /**
         * Formats a duration in seconds to a minutes:seconds string.
         * @param seconds Duration in seconds
         * @return Formatted duration string
         */
        private fun formatDuration(seconds: Int): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return itemView.context.getString(R.string.minutes_seconds, minutes, remainingSeconds)
        }
    }

    /**
     * Callback class for calculating differences between old and new track lists.
     * Used by DiffUtil to efficiently update the RecyclerView.
     */
    private class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }
    }
} 