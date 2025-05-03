package com.example.musicalquiz.viewmodel

import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.model.Album
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.launch

/**
 * ViewModel for the details screen.
 * Manages the display of track or album details and related operations.
 */
class DetailsViewModel : ViewModel() {
    private val api = RetrofitInstance.api
    private var mediaPlayer: MediaPlayer? = null

    // LiveData for track details
    private val _track = MutableLiveData<Track>()
    val track: LiveData<Track> = _track

    // LiveData for album details
    private val _album = MutableLiveData<Album>()
    val album: LiveData<Album> = _album

    // LiveData for album tracks
    private val _albumTracks = MutableLiveData<List<Track>>()
    val albumTracks: LiveData<List<Track>> = _albumTracks

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Preview state
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    // Current track being previewed
    private var currentPreviewTrack: Track? = null

    init {
        _isPlaying.value = false
    }

    /**
     * Loads track details and updates the UI state.
     * @param trackId ID of the track to load
     */
    fun loadTrackDetails(trackId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = api.getTrack(trackId)
                if (response.isSuccessful) {
                    response.body()?.let { track ->
                        _track.value = track
                    } ?: run {
                        _error.value = "Track not found"
                    }
                } else {
                    _error.value = "Failed to load track: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error loading track: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads album details and updates the UI state.
     * @param albumId ID of the album to load
     */
    fun loadAlbumDetails(albumId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Load album details
                val albumResponse = api.getAlbum(albumId)
                if (albumResponse.isSuccessful) {
                    albumResponse.body()?.let { album ->
                        _album.value = album
                    } ?: run {
                        _error.value = "Album not found"
                        return@launch
                    }
                } else {
                    _error.value = "Failed to load album: ${albumResponse.code()}"
                    return@launch
                }

                // Load album tracks
                val tracksResponse = api.getAlbumTracks(albumId)
                if (tracksResponse.isSuccessful) {
                    tracksResponse.body()?.let { response ->
                        _albumTracks.value = response.data
                    }
                } else {
                    _error.value = "Failed to load album tracks: ${tracksResponse.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error loading album: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggles between play and pause states for the current preview.
     */
    fun togglePlayPause() {
        when {
            mediaPlayer?.isPlaying == true -> {
                mediaPlayer?.pause()
                _isPlaying.value = false
            }
            mediaPlayer != null -> {
                mediaPlayer?.start()
                _isPlaying.value = true
            }
        }
    }

    /**
     * Stops the current preview playback.
     */
    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        currentPreviewTrack = null
    }

    /**
     * Plays the track preview.
     * @param track Track to play preview for
     */
    fun playPreview(track: Track) {
        if (track.preview == null) {
            _error.value = "No preview available for this track"
            return
        }

        // If the same track is already playing, toggle play/pause
        if (currentPreviewTrack?.id == track.id) {
            togglePlayPause()
            return
        }

        // Stop any existing playback
        stopPlayback()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(track.preview)
                prepareAsync()
                setOnPreparedListener {
                    try {
                    start()
                    _isPlaying.value = true
                    currentPreviewTrack = track
                    } catch (e: Exception) {
                        _error.value = "Error starting preview: ${e.message}"
                        _isPlaying.value = false
                        currentPreviewTrack = null
                    }
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    currentPreviewTrack = null
                }
                setOnErrorListener { _, what, extra ->
                    val errorMessage = when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> "Unknown error"
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Server died"
                        else -> "Error code: $what, Extra: $extra"
                    }
                    _error.value = "Error playing preview: $errorMessage"
                    _isPlaying.value = false
                    currentPreviewTrack = null
                    true
                }
            }
        } catch (e: Exception) {
            _error.value = "Error initializing preview: ${e.message}"
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    /**
     * Adds a track to the current playlist.
     * @param track Track to add to playlist
     */
    fun addToPlaylist(track: Track) {
        viewModelScope.launch {
            try {
                // TODO: Implement adding track to playlist
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
} 