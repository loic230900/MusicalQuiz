package com.example.musicalquiz.viewmodel

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val playlistDao = database.playlistDao()
    private val playlistTrackDao = database.playlistTrackDao()
    private val api = RetrofitInstance.api

    private companion object { private const val TAG = "PlaylistViewModel" }

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

    private val _playlistArtistCoverImageUrls = MutableLiveData<Map<Int, String?>>(emptyMap()) // New LiveData
    val playlistArtistCoverImageUrls: LiveData<Map<Int, String?>> = _playlistArtistCoverImageUrls

    enum class SortOrder {
        NAME_ASC, NAME_DESC, TRACK_COUNT_ASC, TRACK_COUNT_DESC, DURATION_ASC, DURATION_DESC
    }

    private var currentSortOrder = SortOrder.NAME_ASC

    init {
        viewModelScope.launch {
            playlistDao.getAllPlaylists().collectLatest { playlists ->
                _playlists.postValue(sortPlaylists(playlists))
            }
        }

        viewModelScope.launch {
            _playlists.asFlow().collectLatest { playlists ->
                if (playlists.isEmpty()) {
                    _playlistTrackCounts.postValue(emptyMap())
                    _playlistDurations.postValue(emptyMap())
                    _playlistArtistCoverImageUrls.postValue(emptyMap()) // Reset artist covers
                    return@collectLatest
                }

                val newCounts = mutableMapOf<Int, Int>()
                val newDurations = mutableMapOf<Int, Int>()
                val newArtistCoverUrls = mutableMapOf<Int, String?>() // For artist pictures

                playlists.forEach { playlist ->
                    try {
                        val tracksInDb = playlistTrackDao.getTracksForPlaylist(playlist.id).first()
                        newCounts[playlist.id] = tracksInDb.size
                        newDurations[playlist.id] = tracksInDb.sumOf { it.duration }

                        val firstPlaylistTrack = tracksInDb.minByOrNull { it.position }
                        if (firstPlaylistTrack != null) {
                            newArtistCoverUrls[playlist.id] = fetchArtistPictureForTrack(firstPlaylistTrack.trackId)
                        } else {
                            newArtistCoverUrls[playlist.id] = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing playlist ${playlist.id} details in init", e)
                        newCounts[playlist.id] = 0
                        newDurations[playlist.id] = 0
                        newArtistCoverUrls[playlist.id] = null
                    }
                }
                _playlistTrackCounts.postValue(newCounts)
                _playlistDurations.postValue(newDurations)
                _playlistArtistCoverImageUrls.postValue(newArtistCoverUrls) // Post artist covers

                if (currentSortOrder == SortOrder.DURATION_ASC || currentSortOrder == SortOrder.DURATION_DESC ||
                    currentSortOrder == SortOrder.TRACK_COUNT_ASC || currentSortOrder == SortOrder.TRACK_COUNT_DESC) {
                    _playlists.value?.let { currentSortedPlaylists ->
                        _playlists.postValue(sortPlaylists(currentSortedPlaylists))
                    }
                }
            }
        }
    }

    private suspend fun fetchArtistPictureForTrack(trackId: Long): String? {
        return try {
            val response = api.getTrack(trackId)
            if (response.isSuccessful) {
                val artistPictureUrl = response.body()?.artist?.picture
                Log.d(TAG, "Fetched artist picture for track $trackId: $artistPictureUrl")
                artistPictureUrl // This comes from your Artist.kt model's 'picture' field
            } else {
                Log.w(TAG, "API error fetching track $trackId for artist picture: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching track $trackId for artist picture", e)
            null
        }
    }

    private fun sortPlaylists(playlists: List<Playlist>): List<Playlist> {
        return when (currentSortOrder) {
            SortOrder.NAME_ASC -> playlists.sortedBy { it.name }
            SortOrder.NAME_DESC -> playlists.sortedByDescending { it.name }
            SortOrder.TRACK_COUNT_ASC -> playlists.sortedBy { _playlistTrackCounts.value?.get(it.id) ?: 0 }
            SortOrder.TRACK_COUNT_DESC -> playlists.sortedByDescending { _playlistTrackCounts.value?.get(it.id) ?: 0 }
            SortOrder.DURATION_ASC -> playlists.sortedBy { _playlistDurations.value?.get(it.id) ?: 0 }
            SortOrder.DURATION_DESC -> playlists.sortedByDescending { _playlistDurations.value?.get(it.id) ?: 0 }
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

    suspend fun createPlaylist(name: String): Int {
        _isLoading.postValue(true)
        return try {
            val playlist = Playlist(name = name)
            val newId = playlistDao.insertPlaylist(playlist).toInt()
            val currentArtistCovers = _playlistArtistCoverImageUrls.value?.toMutableMap() ?: mutableMapOf()
            currentArtistCovers[newId] = null // Initially no cover
            _playlistArtistCoverImageUrls.postValue(currentArtistCovers)
            newId
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
            val currentArtistCovers = _playlistArtistCoverImageUrls.value?.toMutableMap() ?: mutableMapOf()
            currentArtistCovers.remove(playlist.id)
            _playlistArtistCoverImageUrls.postValue(currentArtistCovers)
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Long, duration: Int = 0) {
        _isLoading.postValue(true)
        try {
            val existingTracks = playlistTrackDao.getTracksForPlaylist(playlistId).first()
            val currentTrackCountBeforeAdd = existingTracks.size

            val playlistTrack = PlaylistTrack(playlistId = playlistId, trackId = trackId, position = currentTrackCountBeforeAdd, duration = duration)
            playlistTrackDao.insertTrackAtPosition(playlistTrack)

            val updatedTracksInDb = playlistTrackDao.getTracksForPlaylist(playlistId).first()
            val newTotalDuration = updatedTracksInDb.sumOf { it.duration }
            val newTrackCountFromDb = updatedTracksInDb.size

            val currentCounts = _playlistTrackCounts.value?.toMutableMap() ?: mutableMapOf()
            val currentDurations = _playlistDurations.value?.toMutableMap() ?: mutableMapOf()
            currentCounts[playlistId] = newTrackCountFromDb
            currentDurations[playlistId] = newTotalDuration
            _playlistTrackCounts.postValue(currentCounts)
            _playlistDurations.postValue(currentDurations)

            if (currentTrackCountBeforeAdd == 0) { // If it was the first track
                val newArtistCoverUrl = fetchArtistPictureForTrack(trackId)
                val currentArtistCovers = _playlistArtistCoverImageUrls.value?.toMutableMap() ?: mutableMapOf()
                currentArtistCovers[playlistId] = newArtistCoverUrl
                _playlistArtistCoverImageUrls.postValue(currentArtistCovers)
            }

            if (currentSortOrder == SortOrder.DURATION_ASC || currentSortOrder == SortOrder.DURATION_DESC ||
                currentSortOrder == SortOrder.TRACK_COUNT_ASC || currentSortOrder == SortOrder.TRACK_COUNT_DESC) {
                _playlists.value?.let { playlists -> _playlists.postValue(sortPlaylists(playlists)) }
            }
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Long) {
        _isLoading.postValue(true)
        try {
            val trackToDelete = playlistTrackDao.getTracksForPlaylist(playlistId).first().find { it.trackId == trackId }
            if (trackToDelete != null) {
                val wasFirstTrack = trackToDelete.position == 0
                playlistTrackDao.deleteTrack(trackToDelete)

                val updatedTracksInDb = playlistTrackDao.getTracksForPlaylist(playlistId).first()
                val newTrackCount = updatedTracksInDb.size
                val newTotalDuration = updatedTracksInDb.sumOf { it.duration }

                val currentCounts = _playlistTrackCounts.value?.toMutableMap() ?: mutableMapOf()
                val currentDurations = _playlistDurations.value?.toMutableMap() ?: mutableMapOf()
                currentCounts[playlistId] = newTrackCount
                currentDurations[playlistId] = newTotalDuration
                _playlistTrackCounts.postValue(currentCounts)
                _playlistDurations.postValue(currentDurations)

                if (wasFirstTrack || newTrackCount == 0) {
                    val firstPlaylistTrack = updatedTracksInDb.minByOrNull { it.position }
                    val newArtistCoverUrl = firstPlaylistTrack?.let { fetchArtistPictureForTrack(it.trackId) }
                    val currentArtistCovers = _playlistArtistCoverImageUrls.value?.toMutableMap() ?: mutableMapOf()
                    currentArtistCovers[playlistId] = newArtistCoverUrl
                    _playlistArtistCoverImageUrls.postValue(currentArtistCovers)
                }
            }
            if (currentSortOrder == SortOrder.DURATION_ASC || currentSortOrder == SortOrder.DURATION_DESC ||
                currentSortOrder == SortOrder.TRACK_COUNT_ASC || currentSortOrder == SortOrder.TRACK_COUNT_DESC) {
                _playlists.value?.let { playlists -> _playlists.postValue(sortPlaylists(playlists)) }
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
                                if (playlistTrack.duration != track.duration) {
                                    withContext(Dispatchers.IO){ playlistTrackDao.updateTrackDuration(playlistId, track.id, track.duration) }
                                }
                            }
                        }
                    } catch (e: Exception) { /* Skip */ }
                }
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
            // Reordering logic is complex.
            // After reordering, the first track might change.
            viewModelScope.launch {
                val tracksInDb = playlistTrackDao.getTracksForPlaylist(playlistId).first()
                val firstPlaylistTrack = tracksInDb.minByOrNull { it.position }
                val newArtistCoverUrl = firstPlaylistTrack?.let { fetchArtistPictureForTrack(it.trackId) }
                val currentArtistCovers = _playlistArtistCoverImageUrls.value?.toMutableMap() ?: mutableMapOf()
                currentArtistCovers[playlistId] = newArtistCoverUrl
                _playlistArtistCoverImageUrls.postValue(currentArtistCovers)
            }
        } finally {
            _isLoading.postValue(false)
        }
    }

    fun getPlaylistStats(playlistId: Int): PlaylistStats {
        val trackCount = _playlistTrackCounts.value?.get(playlistId) ?: 0
        val duration = _playlistDurations.value?.get(playlistId) ?: 0
        return PlaylistStats(trackCount, duration)
    }

    data class PlaylistStats(val trackCount: Int, val totalDuration: Int)
}
