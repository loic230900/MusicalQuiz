package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.databinding.PlaylistItemBinding
import com.example.musicalquiz.viewmodel.PlaylistViewModel

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onEditClick: (Playlist) -> Unit,
    private val onDeleteClick: (Playlist) -> Unit,
    private var trackCounts: Map<Int, Int> = emptyMap(),
    private var durations: Map<Int, Int> = emptyMap(),
    private var artistCoverImageUrls: Map<Int, String?> = emptyMap() // New map for artist pictures


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

    fun updateArtistCoverImageUrls(newUrls: Map<Int, String?>) {
        if (artistCoverImageUrls != newUrls) {
            this.artistCoverImageUrls = newUrls
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
                
                // Format duration as HH:MM:SS if over an hour, otherwise MM:SS
                val hours = duration / 3600
                val minutes = (duration % 3600) / 60
                val seconds = duration % 60
                val durationText = if (hours > 0) {
                    String.format("%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }
                
                trackCount.text = itemView.context.resources.getQuantityString(
                    R.plurals.track_count,
                    count,
                    count
                )
                playlistDuration.text = durationText

                // Load artist cover image
                val imageUrl = artistCoverImageUrls[playlist.id]
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_playlist)
                    .error(R.drawable.ic_playlist)
                    .into(playlistArtistCoverImage)
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