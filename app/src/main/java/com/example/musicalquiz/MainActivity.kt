package com.example.musicalquiz

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.viewModels
import com.example.musicalquiz.viewmodel.TracksViewModel


class MainActivity : AppCompatActivity() {
    // Utilisation de la propriété viewModels() pour instancier le ViewModel
    private val viewModel: TracksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Appliquer les marges pour gérer les barres de statut et de navigation
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Observer les données : seront rappelées après rotation si déjà chargées
        viewModel.tracksResponse.observe(this) { response ->
            Log.d("LIVEDATA_TEST", "Nombre de résultats : ${response.data.size}")
        }

        // Lancer une recherche si aucune donnée n’existe encore
        if (viewModel.tracksResponse.value == null) {
            viewModel.searchTracks("Eminem")
        }
    }
}