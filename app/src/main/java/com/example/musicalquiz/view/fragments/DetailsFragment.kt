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
 * Fragment affichant les détails d'un morceau ou d'un album.
 * Inclura un aperçu audio si l'élément est une piste.
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

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
            .setView(dialogBinding)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreatePlaylistDialog(onCreated: (com.example.musicalquiz.database.entities.Playlist) -> Unit) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_playlist, null)
        val nameInput = dialogBinding.findViewById<TextInputEditText>(R.id.playlistNameInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(dialogBinding)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val playlistId = playlistViewModel.createPlaylist(name)
                        playlistViewModel.playlists.value?.find { it.id == playlistId }?.let { playlist ->
                            onCreated(playlist)
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private suspend fun addTrackToPlaylist(playlist: com.example.musicalquiz.database.entities.Playlist, trackId: Long) {
        try {
            viewModel.track.value?.let { track ->
                val duration = track.duration
                playlistViewModel.addTrackToPlaylist(playlist.id, trackId, duration)
                Snackbar.make(
                    requireView(),
                    getString(R.string.add_to_playlist_success, playlist.name),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                R.string.add_to_playlist_error,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun addAlbumToPlaylist(playlist: com.example.musicalquiz.database.entities.Playlist, album: com.example.musicalquiz.model.Album) {
        try {
            // Get album tracks from Deezer API
            val tracks = viewModel.getAlbumTracks(album.id)
            var addedCount = 0
            
            tracks.forEach { track ->
                val duration = track.duration
                playlistViewModel.addTrackToPlaylist(playlist.id, track.id, duration)
                addedCount++
            }

            Snackbar.make(
                requireView(),
                getString(R.string.tracks_added, addedCount),
                Snackbar.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                R.string.add_to_playlist_error,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        viewModel.track.observe(viewLifecycleOwner) { track ->
            track?.let { updateTrackUI(it) }
        }

        viewModel.album.observe(viewLifecycleOwner) { album ->
            album?.let { updateAlbumUI(it) }
        }

        viewModel.albumTracks.observe(viewLifecycleOwner) { tracks ->
            trackListAdapter.submitList(tracks)
        }

        viewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            updatePlayPauseButton(isPlaying)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: Show loading indicator
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(
                    requireView(),
                    error,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadTrackDetails() {
        viewModel.loadTrackDetails(itemId)
    }

    private fun loadAlbumDetails() {
        viewModel.loadAlbumDetails(itemId)
    }

    private fun updateTrackUI(track: Track) {
        titleText.text = getString(R.string.track_title_label, track.title)
        artistText.text = getString(R.string.track_artist_label, track.artist.name)
        durationText.text = formatDuration(track.duration)
        
        // Load cover image with Glide
        Glide.with(this)
            .load(track.album.cover)
            .into(coverImage)

        // Update play button state based on preview availability
        playPauseButton.visibility = if (track.preview != null) View.VISIBLE else View.GONE
    }

    private fun updateAlbumUI(album: com.example.musicalquiz.model.Album) {
        titleText.text = getString(R.string.album_title_label, album.title)
        artistText.text = getString(R.string.album_artist_label, album.artist.name)
        
        // Load cover image with Glide
        Glide.with(this)
            .load(album.cover)
            .into(coverImage)
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseButton.setImageResource(
            if (isPlaying) R.drawable.ic_pause
            else R.drawable.ic_play
        )
    }

    private fun navigateToTrackDetails(trackId: Long) {
        val bundle = Bundle().apply {
            putLong("itemId", trackId)
            putBoolean("isTrack", true)
        }
        findNavController().navigate(R.id.navigation_details, bundle)
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "Duration: ${getString(R.string.minutes_seconds, minutes, remainingSeconds)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopPlayback()
    }
}
