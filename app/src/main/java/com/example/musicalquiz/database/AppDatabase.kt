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
 * This Room database manages all persistent data including:
 * - Playlists and their tracks
 * - Quizzes and quiz questions
 * 
 * The database uses Room's singleton pattern to ensure a single instance
 * throughout the application lifecycle.
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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Data Access Object for managing playlists
     */
    abstract fun playlistDao(): PlaylistDao

    /**
     * Data Access Object for managing playlist-track relationships
     */
    abstract fun playlistTrackDao(): PlaylistTrackDao

    /**
     * Data Access Object for managing quizzes
     */
    abstract fun quizDao(): QuizDao

    /**
     * Data Access Object for managing quiz questions
     */
    abstract fun quizQuestionDao(): QuizQuestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets the singleton instance of the database.
         * Creates a new instance if one doesn't exist.
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
