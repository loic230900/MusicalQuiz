package com.example.musicalquiz

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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

        // Test API Deezer : recherche des pistes pour "Eminem"
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.searchTracks("Eminem")
                Log.d("API_TEST", "Résultats reçus : ${response.data}")
            } catch (e: Exception) {
                Log.e("API_TEST", "Erreur API : ${e.message}")
            }
        }
    }
}