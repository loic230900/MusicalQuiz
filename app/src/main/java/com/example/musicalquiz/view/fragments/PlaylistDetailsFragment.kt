package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.TrackListAdapter
import com.example.musicalquiz.databinding.FragmentPlaylistDetailsBinding
import com.example.musicalquiz.viewmodel.DetailsViewModel
import com.example.musicalquiz.viewmodel.PlaylistViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Fragment responsible for displaying and managing playlist details.
 * This fragment provides:
 * - Display of tracks in a playlist
 * - Track preview functionality
 * - Track deletion with confirmation
 * - Navigation to track details
 * - Empty state handling
 * 
 * The fragment uses ViewModels for data management and adapters for displaying
 * tracks in a RecyclerView.
 */
class PlaylistDetailsFragment : Fragment() {
    private var _binding: FragmentPlaylistDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val detailsViewModel: DetailsViewModel by activityViewModels()
    private val args: PlaylistDetailsFragmentArgs by navArgs()
    private lateinit var adapter: TrackListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        viewLifecycleOwner.lifecycleScope.launch {
            playlistViewModel.loadPlaylistTracks(args.playlistId)
        }
    }

    /**
     * Sets up the RecyclerView with the track list adapter and configures click handlers.
     * The adapter handles:
     * - Track selection (navigates to details)
     * - Preview playback
     * - Track deletion
     */
    private fun setupRecyclerView() {
        adapter = TrackListAdapter(
            onTrackClick = { track ->
                val bundle = Bundle().apply {
                    putLong("itemId", track.id)
                    putBoolean("isTrack", true)
                }
                findNavController().navigate(
                    R.id.action_playlistDetailsFragment_to_navigation_details,
                    bundle
                )
            },
            onPreviewClick = { track ->
                detailsViewModel.playPreview(track)
            },
            onDeleteClick = { track ->
                showDeleteConfirmationDialog(track)
            }
        )

        binding.tracksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PlaylistDetailsFragment.adapter
        }
    }

    /**
     * Sets up observers for ViewModel LiveData objects to update the UI.
     * Observes:
     * - Current playlist tracks
     * - Loading state
     * - Playback state
     * - Error messages
     */
    private fun setupObservers() {
        playlistViewModel.currentPlaylistTracks.observe(viewLifecycleOwner) { tracks ->
            adapter.submitList(tracks)
            updateEmptyState(tracks.isEmpty())
        }
        playlistViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show loading indicator if needed
        }
        detailsViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            // Update UI if needed
        }
        detailsViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting a track from the playlist.
     * @param track The track to be deleted
     */
    private fun showDeleteConfirmationDialog(track: com.example.musicalquiz.model.Track) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_track)
            .setMessage(getString(R.string.delete_track_confirmation, track.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        playlistViewModel.removeTrackFromPlaylist(args.playlistId, track.id)
                        Snackbar.make(
                            requireView(),
                            getString(R.string.track_deleted),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Snackbar.make(
                            requireView(),
                            R.string.delete_track_error,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Updates the UI to show either the empty state or the track list.
     * @param isEmpty Whether the playlist is empty
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.tracksRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Refresh playlist data when fragment becomes visible
        viewLifecycleOwner.lifecycleScope.launch {
            playlistViewModel.loadPlaylistTracks(args.playlistId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detailsViewModel.stopPlayback()
        _binding = null
    }
} 