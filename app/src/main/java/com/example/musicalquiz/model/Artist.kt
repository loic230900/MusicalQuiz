package com.example.musicalquiz.model

/**
 * Class representing a music artist.
 *
 * @property id Artist identifier
 * @property name Artist name
 * @property picture URL of the artist's profile picture
 */
data class Artist(
    val id: String,
    val name: String,
    val picture: String
)
