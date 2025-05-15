package com.example.musicalquiz.model

/**
 * Class representing a music track returned by the Deezer API.
 *
 * @property id Unique track identifier
 * @property title Track title
 * @property duration Track duration in seconds
 * @property artist Artist associated with the track
 * @property album Album the track belongs to
 * @property releaseDate Album release date in ISO 8601 format (nullable)
 * @property preview URL to 30s mp3 preview
 */
data class Track(
    val id: Long,
    val title: String,
    val duration: Int, // Duration in seconds
    val artist: Artist,
    val album: Album,
    val releaseDate: String?,
    val preview: String?
) {
    val isAlbum: Boolean
        get() = album.title.equals(title, ignoreCase = true)

    // Helper function to format duration
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}
