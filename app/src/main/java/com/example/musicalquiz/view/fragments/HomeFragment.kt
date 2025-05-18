package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.databinding.FragmentHomeBinding // Ensure this matches your XML file name
import com.example.musicalquiz.databinding.PlaylistItemCondensedBinding
import com.example.musicalquiz.viewmodel.PlaylistViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val playlistViewModel: PlaylistViewModel by viewModels()
    private lateinit var yourPlaylistsAdapter: CondensedPlaylistAdapterHome

    companion object {
        private const val TAG = "HomeFragment_DEBUG"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")


        setupYourPlaylistsRecyclerView()
        setupClickListeners()
        observeViewModels()
    }

    private fun setupYourPlaylistsRecyclerView() {
        Log.d(TAG, "Setting up Your Playlists RecyclerView")
        yourPlaylistsAdapter = CondensedPlaylistAdapterHome { playlist ->
            Log.d(TAG, "Playlist clicked: ${playlist.name}")
            // Navigate to playlist details. Ensure this action/ID exists in nav_graph.
            findNavController().navigate(R.id.navigation_playlist_details, Bundle().apply { putInt("playlistId", playlist.id) })
        }
        binding.yourPlaylistsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = yourPlaylistsAdapter
        }
    }

    private fun observeViewModels() {
        Log.d(TAG, "Setting up observers")
        playlistViewModel.playlists.observe(viewLifecycleOwner, Observer { playlists ->
            Log.d(TAG, "Observed ${playlists.size} playlists from PlaylistViewModel")
            val homeScreenPlaylists = playlists.sortedByDescending { it.createdAt }.take(3)
            if (homeScreenPlaylists.isNotEmpty()) {
                Log.d(TAG, "Displaying ${homeScreenPlaylists.size} playlists on home.")
                binding.yourPlaylistsSection.visibility = View.VISIBLE
                yourPlaylistsAdapter.submitList(homeScreenPlaylists)
            } else {
                Log.d(TAG, "No playlists to display on home.")
                binding.yourPlaylistsSection.visibility = View.GONE
            }
        })

        playlistViewModel.playlistTrackCounts.observe(viewLifecycleOwner, Observer { counts ->
            Log.d(TAG, "Observed playlistTrackCounts: ${counts.size} entries")
            yourPlaylistsAdapter.updateTrackCounts(counts)
        })

        playlistViewModel.playlistArtistCoverImageUrls.observe(viewLifecycleOwner, Observer { artistPictureUrls ->
            Log.d(TAG, "Observed playlistArtistCoverImageUrls: ${artistPictureUrls.size} entries")
            yourPlaylistsAdapter.updateArtistPictureUrls(artistPictureUrls)
        })
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")
        binding.viewAllPlaylistsButton.setOnClickListener {
            Log.d(TAG, "View All Playlists clicked")
            findNavController().navigate(R.id.navigation_playlist)
        }
        binding.createQuizCard.setOnClickListener {
            Log.d(TAG, "Create a New Quiz card clicked")
            findNavController().navigate(R.id.navigation_quiz) // Navigates to QuizFragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        if (::yourPlaylistsAdapter.isInitialized) {
            binding.yourPlaylistsRecyclerView.adapter = null
        }
        _binding = null
    }
}

class CondensedPlaylistAdapterHome(private val onClick: (Playlist) -> Unit) :
    ListAdapter<Playlist, CondensedPlaylistAdapterHome.PlaylistViewHolder>(PlaylistDiffCallbackHome()) {

    private var trackCounts: Map<Int, Int> = emptyMap()
    private var artistPictureUrls: Map<Int, String?> = emptyMap()

    fun updateTrackCounts(newCounts: Map<Int, Int>) {
        trackCounts = newCounts
        notifyDataSetChanged()
    }

    fun updateArtistPictureUrls(newUrls: Map<Int, String?>) {
        artistPictureUrls = newUrls
        notifyDataSetChanged()
    }

    class PlaylistViewHolder(private val binding: PlaylistItemCondensedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: Playlist, trackCount: Int?, artistPictureUrl: String?, onClick: (Playlist) -> Unit) {
            binding.playlistNameTextView.text = playlist.name
            binding.trackCountTextView.text = trackCount?.let {
                itemView.context.resources.getQuantityString(R.plurals.track_count, it, it)
            } ?: itemView.context.getString(R.string.track_number)

            Glide.with(itemView.context)
                .load(artistPictureUrl)
                .placeholder(R.drawable.ic_playlist)
                .error(R.drawable.ic_playlist)
                .into(binding.playlistArtistImageView) // ID from playlist_item_condensed.xml

            itemView.setOnClickListener { onClick(playlist) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = PlaylistItemCondensedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist, trackCounts[playlist.id], artistPictureUrls[playlist.id], onClick)
    }

    class PlaylistDiffCallbackHome : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist): Boolean {
            return oldItem == newItem
        }
    }
}
