package com.example.musicalquiz

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.musicalquiz.viewmodel.TracksViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main activity of the MusicalQuiz application.
 * This activity serves as the entry point of the application and handles:
 * - Bottom navigation setup and management
 * - Main navigation graph hosting
 * - Edge-to-edge display configuration
 * - System insets handling
 * - Search results observation
 * 
 * The activity uses a single activity architecture pattern where
 * different screens are implemented as fragments and navigation
 * is handled through the Navigation component.
 */
class MainActivity : AppCompatActivity() {
    private val viewModel: TracksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, insets.top, 0, 0) // Only apply top insets!
            windowInsets
        }

        // Observe search results
        viewModel.tracksLiveData.observe(this) { tracks ->
            Log.d("LIVEDATA_TEST", "Tracks: ${tracks.size}")
        }

        viewModel.albumsLiveData.observe(this) { albums ->
            Log.d("LIVEDATA_TEST", "Albums: ${albums.size}")
        }
    }
}
