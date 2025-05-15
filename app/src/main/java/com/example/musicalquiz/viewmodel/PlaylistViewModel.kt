package com.example.musicalquiz.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.database.AppDatabase
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.database.entities.PlaylistTrack
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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

    private val _playlistDurations = MutableLiveData<Map<Int, Int>>()
    val playlistDurations: LiveData<Map<Int, Int>> = _playlistDurations

    enum class SortOrder {
        NAME_ASC, NAME_DESC, TRACK_COUNT_ASC, TRACK_COUNT_DESC, DURATION_ASC, DURATION_DESC
    }

    private var currentSortOrder = SortOrder.NAME_ASC

    init {
        viewModelScope.launch {
            // Collect playlists
            playlistDao.getAllPlaylists().collectLatest { playlists ->
                _playlists.postValue(sortPlaylists(playlists))
            }
        }

        // Observe track counts and durations for all playlists
        viewModelScope.launch {
            _playlists.asFlow().collectLatest { playlists ->
                val counts = mutableMapOf<Int, Int>()
                val durations = mutableMapOf<Int, Int>()
                
                // Use a single coroutine to update both counts and durations
                playlists.forEach { playlist ->
                    viewModelScope.launch {
                        try {
                            // Get both track count and duration in a single coroutine
                            val trackCount = playlistTrackDao.getTrackCountForPlaylist(playlist.id).first()
                            val tracks = playlistTrackDao.getTracksForPlaylist(playlist.id).first()
                            val totalDuration = tracks.sumOf { it.duration }
                            
                            // Update both maps atomically
                            counts[playlist.id] = trackCount
                            durations[playlist.id] = totalDuration
                            
                            // Post both updates together
                            _playlistTrackCounts.postValue(counts.toMap())
                            _playlistDurations.postValue(durations.toMap())
                            
                            // Only re-sort if we're in a duration-based sort order
                            if (currentSortOrder == SortOrder.DURATION_ASC || currentSortOrder == SortOrder.DURATION_DESC) {
                                _playlists.value?.let { currentPlaylists ->
                                    _playlists.postValue(sortPlaylists(currentPlaylists))
                                }
                            }
                        } catch (e: Exception) {
                            // Handle any errors
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun sortPlaylists(playlists: List<Playlist>): List<Playlist> {
        return when (currentSortOrder) {
            SortOrder.NAME_ASC -> playlists.sortedBy { it.name }
            SortOrder.NAME_DESC -> playlists.sortedByDescending { it.name }
            SortOrder.TRACK_COUNT_ASC -> playlists.sortedBy { playlistTrackCounts.value?.get(it.id) ?: 0 }
            SortOrder.TRACK_COUNT_DESC -> playlists.sortedByDescending { playlistTrackCounts.value?.get(it.id) ?: 0 }
            SortOrder.DURATION_ASC -> playlists.sortedBy { playlistDurations.value?.get(it.id) ?: 0 }
            SortOrder.DURATION_DESC -> playlists.sortedByDescending { playlistDurations.value?.get(it.id) ?: 0 }
        }
    }

    fun setSortOrder(order: SortOrder) {
        if (currentSortOrder != order) {
            currentSortOrder = order
            _playlists.value?.let { playlists ->
                _playlists.postValue(sortPlaylists(playlists))
            }
        }
    }

    private fun loadPlaylistDuration(playlistId: Int, onDurationLoaded: (Int) -> Unit) {
        viewModelScope.launch {
            playlistTrackDao.getTracksForPlaylist(playlistId).collectLatest { playlistTracks ->
                val totalDuration = playlistTracks.sumOf { it.duration }
                onDurationLoaded(totalDuration)
                // Update the durations map immediately
                val currentDurations = _playlistDurations.value?.toMutableMap() ?: mutableMapOf()
                currentDurations[playlistId] = totalDuration
                _playlistDurations.postValue(currentDurations)
            }
        }
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
    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Long, duration: Int = 0) {
        _isLoading.postValue(true)
        try {
            val trackCount = playlistTrackDao.getTrackCountForPlaylist(playlistId).first()
            val playlistTrack = PlaylistTrack(
                playlistId = playlistId,
                trackId = trackId,
                position = trackCount,
                duration = duration
            )
            playlistTrackDao.insertTrackAtPosition(playlistTrack)
            
            // Update both count and duration atomically
            val tracks = playlistTrackDao.getTracksForPlaylist(playlistId).first()
            val totalDuration = tracks.sumOf { it.duration }
            
            val currentCounts = _playlistTrackCounts.value?.toMutableMap() ?: mutableMapOf()
            val currentDurations = _playlistDurations.value?.toMutableMap() ?: mutableMapOf()
            
            currentCounts[playlistId] = trackCount + 1
            currentDurations[playlistId] = totalDuration
            
            // Post updates immediately
            _playlistTrackCounts.postValue(currentCounts)
            _playlistDurations.postValue(currentDurations)
            
            // Force a re-sort if needed
            if (currentSortOrder == SortOrder.DURATION_ASC || currentSortOrder == SortOrder.DURATION_DESC ||
                currentSortOrder == SortOrder.TRACK_COUNT_ASC || currentSortOrder == SortOrder.TRACK_COUNT_DESC) {
                _playlists.value?.let { playlists ->
                    _playlists.postValue(sortPlaylists(playlists))
                }
            }
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Long) {
        _isLoading.postValue(true)
        try {
            val track = PlaylistTrack(playlistId, trackId, 0)
            playlistTrackDao.deleteTrack(track)
            
            // Update both count and duration atomically
            val trackCount = playlistTrackDao.getTrackCountForPlaylist(playlistId).first()
            val tracks = playlistTrackDao.getTracksForPlaylist(playlistId).first()
            val totalDuration = tracks.sumOf { it.duration }
            
            val currentCounts = _playlistTrackCounts.value?.toMutableMap() ?: mutableMapOf()
            val currentDurations = _playlistDurations.value?.toMutableMap() ?: mutableMapOf()
            
            currentCounts[playlistId] = trackCount
            currentDurations[playlistId] = totalDuration
            
            // Post updates immediately
            _playlistTrackCounts.postValue(currentCounts)
            _playlistDurations.postValue(currentDurations)
            
            // Force a re-sort if needed
            if (currentSortOrder == SortOrder.DURATION_ASC || currentSortOrder == SortOrder.DURATION_DESC ||
                currentSortOrder == SortOrder.TRACK_COUNT_ASC || currentSortOrder == SortOrder.TRACK_COUNT_DESC) {
                _playlists.value?.let { playlists ->
                    _playlists.postValue(sortPlaylists(playlists))
                }
            }
        } finally {
            _isLoading.postValue(false)
        }
    }

    fun loadPlaylistTracks(playlistId: Int) {
        viewModelScope.launch {
            playlistTrackDao.getTracksForPlaylist(playlistId).collectLatest { playlistTracks ->
                val tracks = mutableListOf<Track>()
                var totalDuration = 0
                
                for (playlistTrack in playlistTracks) {
                    try {
                        val response = api.getTrack(playlistTrack.trackId)
                        if (response.isSuccessful) {
                            response.body()?.let { track ->
                                tracks.add(track)
                                totalDuration += track.duration
                                
                                // Update the track duration in the database if it's different
                                if (playlistTrack.duration != track.duration) {
                                    playlistTrackDao.updateTrackDuration(playlistId, track.id, track.duration)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip failed tracks
                    }
                }
                
                // Update the total duration for this playlist
                val currentDurations = _playlistDurations.value?.toMutableMap() ?: mutableMapOf()
                currentDurations[playlistId] = totalDuration
                _playlistDurations.postValue(currentDurations)
                
                _currentPlaylistTracks.postValue(tracks)
            }
        }
    }

    fun reorderTracks(playlistId: Int, fromPosition: Int, toPosition: Int) {
        _isLoading.postValue(true)
        try {
            // Implementation for reordering tracks
            // This would require additional DAO methods and logic
        } finally {
            _isLoading.value = false
        }
    }

    fun getPlaylistStats(playlistId: Int): PlaylistStats {
        val trackCount = playlistTrackCounts.value?.get(playlistId) ?: 0
        val duration = playlistDurations.value?.get(playlistId) ?: 0
        return PlaylistStats(trackCount, duration)
    }

    data class PlaylistStats(
        val trackCount: Int,
        val totalDuration: Int // in seconds
    )
}
