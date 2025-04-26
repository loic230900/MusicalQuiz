package com.example.musicalquiz.model

/**
 * Classe représentant une piste musicale retournée par l'API Deezer.
 *
 * @property id Identifiant unique de la piste
 * @property title Titre de la piste
 * @property duration Durée de la piste en secondes
 * @property artist Artiste associé à la piste
 * @property album Album auquel appartient la piste
 */
data class Track(
    val id: String,
    val title: String,
    val duration: String,
    val artist: Artist,
    val album: Album
) {
    val isAlbum: Boolean
        get() = album.title.equals(title, ignoreCase = true)
}
