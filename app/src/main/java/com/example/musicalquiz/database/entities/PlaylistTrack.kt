package com.example.musicalquiz.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity representing a track in a playlist (junction table).
 * @property playlistId ID of the playlist this track belongs to
 * @property trackId ID of the track from Deezer API
 * @property position Position of the track in the playlist
 * @property addedAt Timestamp when the track was added to the playlist
 */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrack(
    val playlistId: Int,
    val trackId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
) 