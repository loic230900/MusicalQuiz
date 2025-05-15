package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.PlaylistAdapter
import com.example.musicalquiz.databinding.FragmentPlaylistBinding
import com.example.musicalquiz.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Fragment de gestion des playlists : création, affichage, suppression.
 */
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
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val bundle = Bundle().apply {
                    putInt("playlistId", playlist.id)
                }
                findNavController().navigate(
                    R.id.action_playlistFragment_to_playlistDetailsFragment,
                    bundle
                )
            },
            onEditClick = { playlist ->
                showEditPlaylistDialog(playlist)
            },
            onDeleteClick = { playlist ->
                showDeleteConfirmationDialog(playlist)
            }
        )

        binding.playlistRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PlaylistFragment.adapter
        }
    }

    private fun setupObservers() {
        viewModel.playlists.observe(viewLifecycleOwner) { playlists ->
                adapter.submitList(playlists)
                updateEmptyState(playlists.isEmpty())
        }
        viewModel.playlistTrackCounts.observe(viewLifecycleOwner) { counts ->
                adapter.updateTrackCounts(counts)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                // TODO: Show loading indicator if needed
        }
    }

    private fun setupClickListeners() {
        binding.addPlaylistFab.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_playlist, null)
        val nameInput = dialogBinding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.playlistNameInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(dialogBinding)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    viewModel.viewModelScope.launch {
                        viewModel.createPlaylist(name)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditPlaylistDialog(playlist: com.example.musicalquiz.database.entities.Playlist) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_playlist, null)
        val nameInput = dialogBinding.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.playlistNameInput)
        nameInput.setText(playlist.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_playlist)
            .setView(dialogBinding)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = nameInput.text.toString()
                if (newName.isNotBlank()) {
                    viewModel.viewModelScope.launch {
                        viewModel.updatePlaylist(playlist.copy(name = newName))
                    }
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
                viewModel.viewModelScope.launch {
                    viewModel.deletePlaylist(playlist)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.playlistRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
