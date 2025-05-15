package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.databinding.PlaylistItemBinding

class PlaylistSelectionAdapter(
    private val onPlaylistSelected: (Playlist) -> Unit
) : ListAdapter<Playlist, PlaylistSelectionAdapter.ViewHolder>(PlaylistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PlaylistItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: PlaylistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPlaylistSelected(getItem(position))
                }
            }
            // Hide action buttons in selection mode
            binding.editButton.visibility = ViewGroup.GONE
            binding.deleteButton.visibility = ViewGroup.GONE
        }

        fun bind(playlist: Playlist) {
            binding.apply {
                playlistName.text = playlist.name
                trackCount.text = "0 tracks" // TODO: Update with actual count
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