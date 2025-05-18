package com.example.musicalquiz.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FillBlanksQuestionType {
    TRACK_TITLE,
    ARTIST_NAME,
    ALBUM_TITLE
}
@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = Quiz::class,
            parentColumns = ["id"],
            childColumns = ["quizId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quizId"), Index("trackId")]
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val quizId: Int,
    val trackId: Long,
    val previewUrl: String,

    val correctAnswer: String,

    // primary details from the track for reference and generation
    val trackTitle: String,
    val artistName: String,
    val albumTitle: String,

    // used for multiple choice questions
    var mcQuestionFocus: String? = null,
    var incorrectOption1: String? = null,
    var incorrectOption2: String? = null,
    var incorrectOption3: String? = null,

    // for fill in blank kind of questions
    val fillBlanksQuestionType: String? = null,
    val fillBlanksPrompt: String? = null,

    val displayOrder: Int = 0
)
