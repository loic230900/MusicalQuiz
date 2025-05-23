package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.PlaylistSelectionAdapter
import com.example.musicalquiz.adapter.TrackListAdapter
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.viewmodel.DetailsViewModel
import com.example.musicalquiz.viewmodel.PlaylistViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Fragment responsible for displaying detailed information about a track or album.
 * This fragment provides:
 * - Track/album cover art display with Glide image loading
 * - Track/album metadata (title, artist, duration)
 * - Preview playback functionality with MediaPlayer
 * - Track list for albums with RecyclerView
 * - Add to playlist functionality with playlist selection dialog
 * - Create new playlist option
 * 
 * The fragment uses ViewModels for data management and adapters for displaying
 * track lists in a RecyclerView. It supports both track and album views with
 * appropriate UI adjustments for each type.
 */
class DetailsFragment : Fragment() {
    private val viewModel: DetailsViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private lateinit var playlistSelectionAdapter: PlaylistSelectionAdapter

    // Views
    private lateinit var coverImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var artistText: TextView
    private lateinit var durationText: TextView
    private lateinit var tracklistRecyclerView: RecyclerView
    private lateinit var playPauseButton: FloatingActionButton
    private lateinit var addToPlaylistButton: MaterialButton

    private lateinit var trackListAdapter: TrackListAdapter

    private var itemId: Long = 0L
    private var isTrack: Boolean = true

    /**
     * Creates and initializes the fragment's view.
     * Inflates the fragment_details layout and returns the root view.
     * 
     * @param inflater LayoutInflater for creating the view
     * @param container Parent view group
     * @param savedInstanceState Saved instance state
     * @return The initialized view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    /**
     * Initializes the fragment after the view is created.
     * Sets up:
     * - View references and initialization
     * - RecyclerView with track list adapter
     * - Click listeners for playback and playlist actions
     * - ViewModel observers for data updates
     * - Content loading based on item type (track or album)
     * 
     * @param view The fragment's view
     * @param savedInstanceState Saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Manual argument extraction
        arguments?.let {
            itemId = it.getLong("itemId")
            isTrack = it.getBoolean("isTrack")
        }
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Load content based on type
        if (isTrack) {
            loadTrackDetails()
        } else {
            loadAlbumDetails()
        }
    }

    /**
     * Initializes all view references and configures their visibility based on content type.
     * For tracks:
     * - Shows play/pause button and duration
     * - Hides track list
     * For albums:
     * - Hides play/pause button and duration
     * - Shows track list
     * 
     * @param view The fragment's view
     */
    private fun initializeViews(view: View) {
        coverImage = view.findViewById(R.id.coverImage)
        titleText = view.findViewById(R.id.titleText)
        artistText = view.findViewById(R.id.artistText)
        durationText = view.findViewById(R.id.durationText)
        tracklistRecyclerView = view.findViewById(R.id.tracklistRecyclerView)
        playPauseButton = view.findViewById(R.id.playPauseButton)
        addToPlaylistButton = view.findViewById(R.id.addToPlaylistButton)

        // Show/hide views based on content type
        if (isTrack) {
            playPauseButton.visibility = View.VISIBLE
            durationText.visibility = View.VISIBLE
            tracklistRecyclerView.visibility = View.GONE
            playPauseButton.setImageResource(R.drawable.ic_play)
        } else {
            playPauseButton.visibility = View.GONE
            durationText.visibility = View.GONE
            tracklistRecyclerView.visibility = View.VISIBLE
        }
    }

    /**
     * Sets up the RecyclerView for displaying track lists.
     * Configures:
     * - TrackListAdapter with click handlers
     * - LinearLayoutManager for vertical scrolling
     * - Click handlers for track selection and preview
     */
    private fun setupRecyclerView() {
        trackListAdapter = TrackListAdapter(
            onTrackClick = { track ->
                navigateToTrackDetails(track.id)
            },
            onPreviewClick = { track ->
                viewModel.playPreview(track)
            },
            onDeleteClick = { /* no-op, not used in details screen */ }
        )
        tracklistRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = trackListAdapter
        }
    }

    /**
     * Sets up click listeners for interactive elements.
     * Configures:
     * - Play/pause button for track preview
     * - Add to playlist button for playlist selection
     */
    private fun setupClickListeners() {
        playPauseButton.setOnClickListener {
            viewModel.track.value?.let { track ->
                viewModel.playPreview(track)
            }
        }

        addToPlaylistButton.setOnClickListener {
            if (isTrack) {
                viewModel.track.value?.let { track ->
                    showPlaylistSelectionDialog(track)
                }
            } else {
                viewModel.album.value?.let { album ->
                    showPlaylistSelectionDialog(album)
                }
            }
        }
    }

    /**
     * Shows a dialog for selecting a playlist to add the current item to.
     * The dialog includes:
     * - List of existing playlists with track counts
     * - Option to create a new playlist
     * - Confirmation handling for playlist selection
     * 
     * @param item The track or album to add to the selected playlist
     */
    private fun showPlaylistSelectionDialog(item: Any) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_select_playlist, null)
        val recyclerView = dialogBinding.findViewById<RecyclerView>(R.id.playlistRecyclerView)

        playlistSelectionAdapter = PlaylistSelectionAdapter(
            onPlaylistSelected = { playlist ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (item) {
                        is Track -> addTrackToPlaylist(playlist, item.id)
                        is com.example.musicalquiz.model.Album -> addAlbumToPlaylist(playlist, item)
                    }
                }
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = playlistSelectionAdapter
        }

        // Observe playlists and track counts
        playlistViewModel.playlists.observe(viewLifecycleOwner) { playlists ->
            playlistSelectionAdapter.submitList(playlists)
        }
        
        // Observe track counts updates
        playlistViewModel.playlistTrackCounts.observe(viewLifecycleOwner) { counts ->
            playlistSelectionAdapter.updateTrackCounts(counts)
        }

        // Create new playlist button
        dialogBinding.findViewById<MaterialButton>(R.id.createNewButton).setOnClickListener {
            showCreatePlaylistDialog { newPlaylist ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (item) {
                        is Track -> addTrackToPlaylist(newPlaylist, item.id)
                        is com.example.musicalquiz.model.Album -> addAlbumToPlaylist(newPlaylist, item)
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_playlist)
            .setView(dialogBinding)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Shows a dialog for creating a new playlist.
     * The dialog includes:
     * - Text input for playlist name
     * - Validation for empty names
     * - Confirmation handling for playlist creation
     * 
     * @param onPlaylistCreated Callback when a new playlist is created
     */
    private fun showCreatePlaylistDialog(onPlaylistCreated: (Int) -> Unit) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_create_playlist, null)
        val nameInput = dialogBinding.findViewById<TextInputEditText>(R.id.playlistNameInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(dialogBinding)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = nameInput.text?.toString()
                if (!name.isNullOrBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val playlistId = playlistViewModel.createPlaylist(name)
                        onPlaylistCreated(playlistId)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Adds a track to the specified playlist.
     * Shows a success or error message using Snackbar.
     * 
     * @param playlistId ID of the playlist to add the track to
     * @param trackId ID of the track to add
     */
    private suspend fun addTrackToPlaylist(playlistId: Int, trackId: Long) {
        try {
            playlistViewModel.addTrackToPlaylist(playlistId, trackId)
            Snackbar.make(
                requireView(),
                R.string.track_added_to_playlist,
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                R.string.error_adding_track,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Adds all tracks from an album to the specified playlist.
     * Shows a success or error message using Snackbar.
     * 
     * @param playlistId ID of the playlist to add the tracks to
     * @param album The album containing the tracks to add
     */
    private suspend fun addAlbumToPlaylist(playlistId: Int, album: com.example.musicalquiz.model.Album) {
        try {
            val tracks = viewModel.getAlbumTracks(album.id)
            for (track in tracks) {
                playlistViewModel.addTrackToPlaylist(playlistId, track.id)
            }
            Snackbar.make(
                requireView(),
                R.string.album_added_to_playlist,
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                R.string.error_adding_album,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Navigates to the details screen for a specific track.
     * 
     * @param trackId ID of the track to show details for
     */
    private fun navigateToTrackDetails(trackId: Long) {
        val bundle = Bundle().apply {
            putLong("itemId", trackId)
            putBoolean("isTrack", true)
        }
        findNavController().navigate(
            R.id.action_detailsFragment_self,
            bundle
        )
    }

    /**
     * Loads track details from the ViewModel and updates the UI.
     * Displays:
     * - Track cover art
     * - Track title and artist
     * - Track duration
     */
    private fun loadTrackDetails() {
        viewModel.loadTrackDetails(itemId)
        viewModel.track.observe(viewLifecycleOwner) { track ->
            track?.let {
                Glide.with(this)
                    .load(it.album.cover)
                    .into(coverImage)
                titleText.text = it.title
                artistText.text = it.artist.name
                durationText.text = formatDuration(it.duration)
            }
        }
    }

    /**
     * Loads album details from the ViewModel and updates the UI.
     * Displays:
     * - Album cover art
     * - Album title and artist
     * - List of tracks in the album
     */
    private fun loadAlbumDetails() {
        viewModel.loadAlbumDetails(itemId)
        viewModel.album.observe(viewLifecycleOwner) { album ->
            album?.let {
                Glide.with(this)
                    .load(it.cover)
                    .into(coverImage)
                titleText.text = it.title
                artistText.text = it.artist.name
            }
        }
        viewModel.albumTracks.observe(viewLifecycleOwner) { tracks ->
            trackListAdapter.submitList(tracks)
        }
    }

    /**
     * Formats a duration in seconds to a human-readable string.
     * Format: MM:SS
     * 
     * @param duration Duration in seconds
     * @return Formatted duration string
     */
    private fun formatDuration(duration: Int): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    /**
     * Sets up observers for ViewModel LiveData objects.
     * Observes:
     * - Loading state
     * - Error messages
     * - Playback state
     */
    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show loading indicator if needed
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
            }
        }
        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPlayback()
    }
}
