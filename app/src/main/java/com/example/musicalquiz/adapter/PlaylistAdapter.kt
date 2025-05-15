package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicalquiz.R
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.databinding.PlaylistItemBinding
import com.example.musicalquiz.viewmodel.PlaylistViewModel

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onEditClick: (Playlist) -> Unit,
    private val onDeleteClick: (Playlist) -> Unit,
    private var trackCounts: Map<Int, Int> = emptyMap(),
    private var durations: Map<Int, Int> = emptyMap()
) : ListAdapter<Playlist, PlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = PlaylistItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateTrackCounts(newCounts: Map<Int, Int>) {
        if (trackCounts != newCounts) {
            this.trackCounts = newCounts
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun updateDurations(newDurations: Map<Int, Int>) {
        if (durations != newDurations) {
            this.durations = newDurations
            notifyItemRangeChanged(0, itemCount)
        }
    }

    inner class PlaylistViewHolder(
        private val binding: PlaylistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistClick(getItem(position))
                }
            }

            binding.editButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(playlist: Playlist) {
            binding.apply {
                playlistName.text = playlist.name
                val count = trackCounts[playlist.id] ?: 0
                val duration = durations[playlist.id] ?: 0
                
                // Format duration as MM:SS
                val minutes = duration / 60
                val seconds = duration % 60
                val durationText = if (duration > 0) {
                    String.format("%d:%02d", minutes, seconds)
                } else {
                    "0:00"
                }
                
                trackCount.text = itemView.context.resources.getQuantityString(
                    R.plurals.track_count,
                    count,
                    count
                )
                playlistDuration.text = durationText
            }
        }
    }

    private class PlaylistDiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
} 