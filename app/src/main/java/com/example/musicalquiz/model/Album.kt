package com.example.musicalquiz.model

/**
 * Classe représentant un album musical.
 *
 * @property id Identifiant de l’album
 * @property title Titre de l’album
 * @property cover URL de l’image de couverture de l’album
 */

data class Album(
    val id: String,
    val title: String,
    val cover: String,
    val artist: Artist
)