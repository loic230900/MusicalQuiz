package com.example.musicalquiz.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.model.Artist
import com.example.musicalquiz.model.Album

/**
 * Entity representing a track in a playlist (junction table).
 * @property playlistId ID of the playlist this track belongs to
 * @property trackId ID of the track from Deezer API
 * @property position Position of the track in the playlist
 * @property duration Duration of the track in seconds
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
    val duration: Int = 0, // Duration in seconds
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track {
        // This will require fetching the track details from the Deezer API
        return Track(
            id = trackId,
            title = "Loading...",
            duration = 0,
            artist = Artist("0", "Loading...", ""),
            album = Album(0, "Loading...", "", Artist("0", "Loading...", ""), "Loading..."),
            releaseDate = "Loading...",
            preview = null
        )
    }
} 