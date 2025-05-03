package com.example.musicalquiz.view.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.TrackListAdapter
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.viewmodel.DetailsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment affichant les détails d'un morceau ou d'un album.
 * Inclura un aperçu audio si l'élément est une piste.
 */
class DetailsFragment : Fragment() {
    private val viewModel: DetailsViewModel by activityViewModels()

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
            }
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
            viewModel.track.value?.let { track ->
                viewModel.addToPlaylist(track)
            }
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
