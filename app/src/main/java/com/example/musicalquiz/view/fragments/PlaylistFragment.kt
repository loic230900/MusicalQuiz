package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer // Ensure this is androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.PlaylistAdapter
import com.example.musicalquiz.databinding.FragmentPlaylistBinding
import com.example.musicalquiz.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class PlaylistFragment : Fragment() {
    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaylistViewModel by viewModels()
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        viewModel.refreshPlaylists() // Initial refresh
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPlaylists() // Refresh when fragment becomes visible
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val action = PlaylistFragmentDirections.actionPlaylistFragmentToPlaylistDetailsFragment(playlist.id)
                findNavController().navigate(action)
            },
            onEditClick = { playlist ->
                showEditPlaylistDialog(playlist)
            },
            onDeleteClick = { playlist ->
                showDeleteConfirmationDialog(playlist)
            },
            // Pass initial empty maps; they will be updated by LiveData
            trackCounts = viewModel.playlistTrackCounts.value ?: emptyMap(),
            durations = viewModel.playlistDurations.value ?: emptyMap(),
            artistCoverImageUrls = viewModel.playlistArtistCoverImageUrls.value ?: emptyMap() // Initialize
        )

        binding.playlistRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PlaylistFragment.adapter
        }
    }

    private fun setupObservers() {
        // Observe playlists
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            adapter.submitList(playlists)
            updateEmptyState(playlists.isEmpty())
        }

        // Observe track counts
        viewModel.playlistTrackCounts.observe(viewLifecycleOwner) { counts ->
            adapter.updateTrackCounts(counts)
        }

        // Observe durations
        viewModel.playlistDurations.observe(viewLifecycleOwner) { durations ->
            adapter.updateDurations(durations)
        }

        // Observe artist cover images
        viewModel.playlistArtistCoverImageUrls.observe(viewLifecycleOwner) { artistCoverUrls ->
            adapter.updateArtistCoverImageUrls(artistCoverUrls)
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.addPlaylistFab.setOnClickListener {
            showCreatePlaylistDialog()
        }
        binding.sortButton?.setOnClickListener {
            showSortOptionsDialog()
        }
    }

    private fun showSortOptionsDialog() {
        val options = arrayOf("Name (A-Z)", "Name (Z-A)", "Track Count (Low to High)", "Track Count (High to Low)", "Duration (Short to Long)", "Duration (Long to Short)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Playlists")
            .setItems(options) { _, which ->
                val sortOrder = when (which) {
                    0 -> PlaylistViewModel.SortOrder.NAME_ASC
                    1 -> PlaylistViewModel.SortOrder.NAME_DESC
                    2 -> PlaylistViewModel.SortOrder.TRACK_COUNT_ASC
                    3 -> PlaylistViewModel.SortOrder.TRACK_COUNT_DESC
                    4 -> PlaylistViewModel.SortOrder.DURATION_ASC
                    5 -> PlaylistViewModel.SortOrder.DURATION_DESC
                    else -> PlaylistViewModel.SortOrder.NAME_ASC
                }
                viewModel.setSortOrder(sortOrder)
            }
            .show()
    }

    private fun showCreatePlaylistDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_playlist, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.playlistNameInput)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(dialogView)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotBlank()) {
                    viewModel.viewModelScope.launch { viewModel.createPlaylist(name) }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditPlaylistDialog(playlist: com.example.musicalquiz.database.entities.Playlist) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_playlist, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.playlistNameInput)
        nameInput.setText(playlist.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_playlist)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = nameInput.text.toString().trim()
                if (newName.isNotBlank()) {
                    viewModel.viewModelScope.launch { viewModel.updatePlaylist(playlist.copy(name = newName)) }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmationDialog(playlist: com.example.musicalquiz.database.entities.Playlist) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_playlist)
            .setMessage(getString(R.string.delete_playlist_confirmation, playlist.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.viewModelScope.launch { viewModel.deletePlaylist(playlist) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.playlistRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.sortButton?.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playlistRecyclerView.adapter = null
        _binding = null
    }
}
