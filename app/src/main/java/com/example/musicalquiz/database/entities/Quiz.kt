package com.example.musicalquiz.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quizzes",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class Quiz(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val playlistId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val questionSelectionMode: String = QuestionSelectionMode.RANDOM.name,
    val gameMode: String = GameMode.MULTIPLE_CHOICE.name,
    val timeLimitPerQuestion: Int? = null,
    var lastPlayed: Long? = null,
    var bestScore: Int? = null
)

enum class QuestionSelectionMode {
    RANDOM,
}


enum class GameMode {
    MULTIPLE_CHOICE,
    FILL_IN_THE_BLANKS,
}
