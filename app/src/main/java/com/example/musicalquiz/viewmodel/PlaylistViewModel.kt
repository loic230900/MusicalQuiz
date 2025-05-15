package com.example.musicalquiz.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.database.AppDatabase
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.database.entities.PlaylistTrack
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel responsable de la gestion des playlists.
 * DAO et logique Room à intégrer à l'étape 6.
 */
class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val playlistDao = database.playlistDao()
    private val playlistTrackDao = database.playlistTrackDao()
    private val api = RetrofitInstance.api

    // State flows -> LiveData
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _playlists = MutableLiveData<List<Playlist>>(emptyList())
    val playlists: LiveData<List<Playlist>> = _playlists

    private val _currentPlaylistTracks = MutableLiveData<List<Track>>(emptyList())
    val currentPlaylistTracks: LiveData<List<Track>> = _currentPlaylistTracks

    private val _playlistTrackCounts = MutableLiveData<Map<Int, Int>>(emptyMap())
    val playlistTrackCounts: LiveData<Map<Int, Int>> = _playlistTrackCounts

    init {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collectLatest { playlists ->
                _playlists.postValue(playlists)
                // Update track counts for all playlists
                updateTrackCounts(playlists)
            }
        }
    }

    private suspend fun updateTrackCounts(playlists: List<Playlist>) {
        val counts = playlists.associate { playlist ->
            playlist.id to playlistTrackDao.getTrackCountForPlaylist(playlist.id)
        }
        _playlistTrackCounts.postValue(counts)
    }

    // CRUD Operations
    suspend fun createPlaylist(name: String): Int {
        _isLoading.postValue(true)
        return try {
            val playlist = Playlist(name = name)
            playlistDao.insertPlaylist(playlist).toInt()
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        _isLoading.postValue(true)
        try {
            playlistDao.updatePlaylist(playlist)
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        _isLoading.postValue(true)
        try {
            playlistDao.deletePlaylist(playlist)
        } finally {
            _isLoading.postValue(false)
        }
    }

    // Track Management
    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Long) {
        _isLoading.postValue(true)
        try {
            val trackCount = playlistTrackDao.getTrackCountForPlaylist(playlistId)
            val playlistTrack = PlaylistTrack(
                playlistId = playlistId,
                trackId = trackId,
                position = trackCount
            )
            playlistTrackDao.insertTrackAtPosition(playlistTrack)
            // Update track count
            updateTrackCounts(_playlists.value ?: emptyList())
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Long) {
        _isLoading.postValue(true)
        try {
            val track = PlaylistTrack(playlistId, trackId, 0)
            playlistTrackDao.deleteTrack(track)
            // Update track count
            updateTrackCounts(_playlists.value ?: emptyList())
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun loadPlaylistTracks(playlistId: Int) {
        viewModelScope.launch {
            playlistTrackDao.getTracksForPlaylist(playlistId).collectLatest { playlistTracks ->
                val tracks = mutableListOf<Track>()
                for (playlistTrack in playlistTracks) {
                    try {
                        val response = api.getTrack(playlistTrack.trackId)
                        if (response.isSuccessful) {
                            response.body()?.let { tracks.add(it) }
                        }
                    } catch (e: Exception) {
                        // Skip failed tracks
                    }
                }
                _currentPlaylistTracks.postValue(tracks)
            }
        }
    }

    suspend fun reorderTracks(playlistId: Int, fromPosition: Int, toPosition: Int) {
        _isLoading.postValue(true)
        try {
            // Implementation for reordering tracks
            // This would require additional DAO methods and logic
        } finally {
            _isLoading.value = false
        }
    }
}
