package com.example.musicalquiz.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicalquiz.database.entities.QuizQuestion
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the QuizQuestion entity.
 * This interface provides methods for database operations related to quiz questions
 * such as inserting, retrieving, and deleting questions associated with specific quizzes
 */

@Dao
interface QuizQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuizQuestion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<QuizQuestion>)

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY displayOrder ASC")
    fun getQuestionsForQuiz(quizId: Int): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY RANDOM()")
    fun getRandomQuestionsForQuiz(quizId: Int): Flow<List<QuizQuestion>> //random selection

    @Query("DELETE FROM quiz_questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsForQuiz(quizId: Int)

    @Query("DELETE FROM quiz_questions WHERE id = :questionId")
    suspend fun deleteQuestionById(questionId: Int)
}
