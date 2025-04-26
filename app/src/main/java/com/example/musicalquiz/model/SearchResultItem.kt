package com.example.musicalquiz.model

sealed class SearchResultItem {
    data class TrackItem(val track: Track) : SearchResultItem()
    data class AlbumItem(val album: Album) : SearchResultItem()
}

// Extension function pour différencier chaque item par un ID unique
fun SearchResultItem.getUniqueId(): String {
    return when (this) {
        is SearchResultItem.TrackItem -> "track_${this.track.id}"
        is SearchResultItem.AlbumItem -> "album_${this.album.id}"
    }
}
