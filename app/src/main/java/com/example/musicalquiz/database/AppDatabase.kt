package com.example.musicalquiz.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.musicalquiz.database.dao.PlaylistDao
import com.example.musicalquiz.database.dao.PlaylistTrackDao
import com.example.musicalquiz.database.dao.QuizDao
import com.example.musicalquiz.database.dao.QuizQuestionDao
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.database.entities.PlaylistTrack
import com.example.musicalquiz.database.entities.Quiz
import com.example.musicalquiz.database.entities.QuizQuestion

/**
 * Main database class for the MusicalQuiz application.
 * This Room database manages:
 * - Playlists and their tracks
 * - Quizzes and quiz questions
 * - User preferences and settings
 * 
 * The database uses Room's DAO pattern for data access and provides
 * type-safe queries through Kotlin coroutines.
 * 
 * @property version Current database version (5)
 * @property exportSchema Whether to export the database schema (disabled)
 */
@Database(
    entities = [
        Playlist::class,
        PlaylistTrack::class,
        Quiz::class,
        QuizQuestion::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Data Access Object for managing playlists.
     * Provides methods for CRUD operations on playlists.
     */
    abstract fun playlistDao(): PlaylistDao

    /**
     * Data Access Object for managing playlist-track relationships.
     * Handles the many-to-many relationship between playlists and tracks.
     */
    abstract fun playlistTrackDao(): PlaylistTrackDao

    /**
     * Data Access Object for managing quizzes.
     * Provides methods for creating and managing music quizzes.
     */
    abstract fun quizDao(): QuizDao

    /**
     * Data Access Object for managing quiz questions.
     * Handles the one-to-many relationship between quizzes and questions.
     */
    abstract fun quizQuestionDao(): QuizQuestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the database.
         * Creates a new instance if one doesn't exist.
         * Uses double-checked locking for thread safety.
         * 
         * @param context Application context
         * @return The singleton database instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "musical_quiz_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
