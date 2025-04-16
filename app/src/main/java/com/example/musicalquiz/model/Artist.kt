package com.example.musicalquiz.model

/**
 * Classe représentant un artiste musical.
 *
 * @property id Identifiant de l’artiste
 * @property name Nom de l’artiste
 * @property picture URL de la photo de profil de l’artiste
 */
data class Artist(
    val id: String,
    val name: String,
    val picture: String
)
