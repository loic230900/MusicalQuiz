package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.AlbumAdapter
import com.example.musicalquiz.adapter.TrackAdapter
import com.example.musicalquiz.adapter.PlaylistSelectionAdapter
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.model.Album
import com.example.musicalquiz.viewmodel.TracksViewModel
import com.example.musicalquiz.viewmodel.PlaylistViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Fragment responsible for the music search interface.
 * This fragment provides:
 * - Search functionality for tracks and albums via the Deezer API
 * - Adaptive grid layout for search results
 * - Filtering between tracks and albums
 * - Infinite scrolling with pagination
 * - Playlist selection for tracks and albums
 * - Empty state handling
 * 
 * The fragment uses ViewModels for data management and adapters for displaying
 * search results in a RecyclerView.
 */
class SearchFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var trackAdapter: TrackAdapter
    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var searchFilterGroup: RadioGroup
    private lateinit var trackFilter: RadioButton
    private lateinit var albumFilter: RadioButton

    private val viewModel: TracksViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private lateinit var playlistSelectionAdapter: PlaylistSelectionAdapter

    /**
     * Creates and initializes the fragment's view.
     * Sets up:
     * - Search bar and button
     * - RecyclerView with adaptive grid layout
     * - Track and album adapters
     * - Infinite scrolling
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
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        
        // Initialiser les vues
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        recyclerView = view.findViewById(R.id.searchRecyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)
        searchFilterGroup = view.findViewById(R.id.searchFilterGroup)
        trackFilter = view.findViewById(R.id.trackFilter)
        albumFilter = view.findViewById(R.id.albumFilter)

        // Configurer le RecyclerView avec le nombre de colonnes adapté à l'orientation
        val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        
        // Add scroll listener for infinite scrolling
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position == layoutManager.itemCount - 1 && viewModel.isLoading.value == true) {
                    spanCount // Loading indicator takes full width
                } else {
                    1 // Normal item takes one column
                }
            }
        }
        
        recyclerView.layoutManager = layoutManager
        
        // Add scroll listener
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!viewModel.isLoading.value!! && 
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                    firstVisibleItemPosition >= 0) {
                    viewModel.loadMoreResults()
                }
            }
        })

        trackAdapter = TrackAdapter().apply {
            setOnItemClickListener { track ->
                onTrackClick(track)
            }
            setOnItemLongClickListener { track ->
                showPlaylistSelectionDialog(track)
            }
        }
        albumAdapter = AlbumAdapter().apply {
            setOnItemClickListener { album ->
                onAlbumClick(album)
            }
            setOnItemLongClickListener { album ->
                showPlaylistSelectionDialog(album)
            }
        }
        recyclerView.adapter = trackAdapter

        return view
    }

    /**
     * Sets up listeners and observers after view creation.
     * Initializes:
     * - Search functionality
     * - Filter controls
     * - LiveData observers
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configurer les listeners
        setupListeners()
        
        // Configurer les observateurs
        setupObservers()
    }

    /**
     * Reloads top charts and resets search bar when returning to the fragment.
     * Called when the fragment becomes visible to the user.
     */
    override fun onResume() {
        super.onResume()
        // Recharger les top charts à chaque fois qu'on revient sur le fragment
        viewModel.loadTopCharts()
        // Réinitialiser la barre de recherche
        searchEditText.text.clear()
    }

    /**
     * Adapts the grid layout when device orientation changes.
     * Updates the number of columns based on screen orientation:
     * - Landscape: 3 columns
     * - Portrait: 2 columns
     * 
     * @param newConfig The new device configuration
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Adapter le nombre de colonnes lors du changement d'orientation
        val spanCount = if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = spanCount
    }

    /**
     * Sets up listeners for user interactions:
     * - Search button and text input
     * - Filter radio buttons
     * - Empty search handling
     */
    private fun setupListeners() {
        searchButton.setOnClickListener {
            triggerSearch()
        }

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                triggerSearch()
                true
            } else false
        }

        // Add text change listener to handle empty search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    viewModel.loadTopCharts()
                }
            }
        })

        searchFilterGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.trackFilter -> {
                    recyclerView.adapter = trackAdapter
                    viewModel.setFilter(TracksViewModel.SearchFilter.TRACKS)
                    // Always reload data when changing filter
                    if (searchEditText.text.isNullOrEmpty()) {
                    viewModel.loadTopCharts()
                    } else {
                        viewModel.searchAll(searchEditText.text.toString())
                    }
                }
                R.id.albumFilter -> {
                    recyclerView.adapter = albumAdapter
                    viewModel.setFilter(TracksViewModel.SearchFilter.ALBUMS)
                    // Always reload data when changing filter
                    if (searchEditText.text.isNullOrEmpty()) {
                    viewModel.loadTopCharts()
                    } else {
                        viewModel.searchAll(searchEditText.text.toString())
                    }
                }
            }
        }
    }

    /**
     * Sets up observers for LiveData from ViewModels:
     * - Search results
     * - Loading state
     * - Error messages
     * - Available playlists
     */
    private fun setupObservers() {
        viewModel.tracksLiveData.observe(viewLifecycleOwner) { tracks ->
            trackAdapter.submitList(tracks)
            updateEmptyState()
        }

        viewModel.albumsLiveData.observe(viewLifecycleOwner) { albums ->
            albumAdapter.submitList(albums)
            updateEmptyState()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            searchButton.isEnabled = !isLoading
            if (isLoading) {
                emptyStateTextView.text = getString(R.string.loading)
                emptyStateTextView.visibility = View.VISIBLE
                searchButton.text = getString(R.string.searching)
            } else {
                searchButton.text = getString(R.string.search)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                emptyStateTextView.text = getString(R.string.error_occurred, error)
                emptyStateTextView.visibility = View.VISIBLE
                // Show error in a Snackbar for better visibility
                Snackbar.make(
                    requireView(),
                    getString(R.string.error_occurred, error),
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.retry) {
                    triggerSearch()
                }.show()
            }
        }
    }

    private fun updateEmptyState() {
        val isTracksEmpty = trackAdapter.itemCount == 0
        val isAlbumsEmpty = albumAdapter.itemCount == 0
        emptyStateTextView.visibility = if (isTracksEmpty && isAlbumsEmpty) View.VISIBLE else View.GONE
    }

    /**
     * Triggers a new search based on current input and filter.
     * Handles:
     * - Input validation
     * - Keyboard dismissal
     * - Search execution
     */
    private fun triggerSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            hideKeyboard()
            viewModel.searchAll(query)
        } else {
            viewModel.loadTopCharts()
        }
    }

    /**
     * Cache le clavier virtuel.
     */
    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    /**
     * Shows a dialog for selecting a playlist to add the item to.
     * Handles both tracks and albums.
     * 
     * @param item The track or album to add to a playlist
     */
    private fun showPlaylistSelectionDialog(item: Any) {
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_select_playlist, null)
        val recyclerView = dialogBinding.findViewById<RecyclerView>(R.id.playlistRecyclerView)

        playlistSelectionAdapter = PlaylistSelectionAdapter(
            onPlaylistSelected = { playlist ->
                viewLifecycleOwner.lifecycleScope.launch {
                    when (item) {
                        is Track -> addTrackToPlaylist(playlist, item)
                        is Album -> addAlbumToPlaylist(playlist, item)
                    }
                }
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = playlistSelectionAdapter
        }

        // Observe playlists
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
                        is Track -> addTrackToPlaylist(newPlaylist, item)
                        is Album -> addAlbumToPlaylist(newPlaylist, item)
                    }
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreatePlaylistDialog(onCreated: (Playlist) -> Unit) {
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

    private suspend fun addTrackToPlaylist(playlist: Playlist, track: Track) {
        try {
            playlistViewModel.addTrackToPlaylist(playlist.id, track.id, track.duration)
            Snackbar.make(
                requireView(),
                getString(R.string.add_to_playlist_success, playlist.name),
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

    private suspend fun addAlbumToPlaylist(playlist: Playlist, album: Album) {
        try {
            // Get album tracks from Deezer API
            val tracks = viewModel.getAlbumTracks(album.id)
            var addedCount = 0
            
            tracks.forEach { track ->
                playlistViewModel.addTrackToPlaylist(playlist.id, track.id, track.duration)
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

    /**
     * Handles click on a track item.
     * Navigates to track details.
     * 
     * @param track The clicked track
     */
    private fun onTrackClick(track: Track) {
        val bundle = Bundle().apply {
            putLong("itemId", track.id)
            putBoolean("isTrack", true)
        }
        findNavController().navigate(R.id.navigation_details, bundle)
    }

    /**
     * Handles click on an album item.
     * Navigates to album details.
     * 
     * @param album The clicked album
     */
    private fun onAlbumClick(album: Album) {
        val bundle = Bundle().apply {
            putLong("itemId", album.id)
            putBoolean("isTrack", false)
        }
        findNavController().navigate(R.id.navigation_details, bundle)
    }
}
