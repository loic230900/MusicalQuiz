package com.example.musicalquiz.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.database.entities.Quiz
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the Quiz entity
 * This interface defines the methods for interacting with the quizzes table in the database,
 * including inserting, updating, deleting, and querying quiz data.
 */

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: Quiz): Long

    @Update
    suspend fun updateQuiz(quiz: Quiz)

    @Delete
    suspend fun deleteQuiz(quiz: Quiz)

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuizById(quizId: Int)

    @Query("SELECT * FROM quizzes WHERE id = :quizId")
    suspend fun getQuizById(quizId: Int): Quiz?



    @Transaction
    @Query("""
        SELECT q.*, p.name as playlistName 
        FROM quizzes q
        INNER JOIN playlists p ON q.playlistId = p.id
        ORDER BY q.createdAt DESC
    """)
    fun getAllQuizzesWithPlaylistName(): Flow<List<QuizWithPlaylistInfo>>

    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzes(): Flow<List<Quiz>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistForQuiz(playlistId: Int): Playlist?
}

/**
 * This data class hold the name of the quiz and associated playlist name
 */
data class QuizWithPlaylistInfo(
    @androidx.room.Embedded
    val quiz: Quiz,
    val playlistName: String
)
