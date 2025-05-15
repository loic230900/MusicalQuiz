package com.example.musicalquiz.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.musicalquiz.database.entities.PlaylistTrack
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the PlaylistTrack entity.
 * Provides methods to interact with the playlist_tracks table.
 */
@Dao
interface PlaylistTrackDao {
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getTracksForPlaylist(playlistId: Int): Flow<List<PlaylistTrack>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getTrackCountForPlaylist(playlistId: Int): Flow<Int>

    @Insert
    suspend fun insertTrack(playlistTrack: PlaylistTrack)

    @Delete
    suspend fun deleteTrack(playlistTrack: PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteAllTracksFromPlaylist(playlistId: Int)

    @Query("UPDATE playlist_tracks SET position = position + 1 WHERE playlistId = :playlistId AND position >= :position")
    suspend fun shiftTracksPositions(playlistId: Int, position: Int)

    @Query("UPDATE playlist_tracks SET duration = :duration WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun updateTrackDuration(playlistId: Int, trackId: Long, duration: Int)

    @Transaction
    suspend fun insertTrackAtPosition(playlistTrack: PlaylistTrack) {
        shiftTracksPositions(playlistTrack.playlistId, playlistTrack.position)
        insertTrack(playlistTrack)
    }
} 