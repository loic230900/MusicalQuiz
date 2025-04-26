package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.MixedItemAdapter
import com.example.musicalquiz.viewmodel.TracksViewModel

/**
 * Fragment responsable de l'interface de recherche de musique.
 * Permet à l'utilisateur de rechercher des morceaux et des albums via l'API Deezer.
 * Affiche les résultats dans une grille adaptative selon l'orientation de l'écran.
 */
class SearchFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView
    private lateinit var adapter: MixedItemAdapter

    private val viewModel: TracksViewModel by activityViewModels()

    /**
     * Crée et initialise la vue du fragment.
     * Configure la barre de recherche et le RecyclerView avec une grille adaptative.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        
        // Initialiser les vues
        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        recyclerView = view.findViewById(R.id.searchRecyclerView)
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView)

        // Configurer le RecyclerView avec le nombre de colonnes adapté à l'orientation
        val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        adapter = MixedItemAdapter(emptyList())
        recyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        recyclerView.adapter = adapter

        return view
    }

    /**
     * Configure les listeners et les observateurs après la création de la vue.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Configurer les listeners
        setupListeners()
        
        // Configurer les observateurs
        setupObservers()
    }

    /**
     * Recharge les top charts et réinitialise la barre de recherche à chaque retour sur le fragment.
     */
    override fun onResume() {
        super.onResume()
        // Recharger les top charts à chaque fois qu'on revient sur le fragment
        viewModel.loadTopCharts()
        // Réinitialiser la barre de recherche
        searchEditText.text.clear()
    }

    /**
     * Adapte le nombre de colonnes de la grille lors d'un changement d'orientation.
     * @param newConfig La nouvelle configuration de l'appareil
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Adapter le nombre de colonnes lors du changement d'orientation
        val spanCount = if (newConfig.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
        (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = spanCount
    }

    /**
     * Configure les listeners pour la barre de recherche et le bouton de recherche.
     */
    private fun setupListeners() {
        searchButton.setOnClickListener {
            triggerSearch()
        }

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                triggerSearch()
                true
            } else false
        }
    }

    /**
     * Configure les observateurs pour les données du ViewModel.
     * Gère l'affichage des résultats, l'état de chargement et les erreurs.
     */
    private fun setupObservers() {
        viewModel.itemsLiveData.observe(viewLifecycleOwner) { items ->
            adapter.updateData(items)
            emptyStateTextView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            searchButton.isEnabled = !isLoading
            if (isLoading) {
                emptyStateTextView.text = getString(R.string.loading)
                emptyStateTextView.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                emptyStateTextView.text = getString(R.string.error_occurred)
                emptyStateTextView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Déclenche une recherche avec le texte saisi dans la barre de recherche.
     * Cache le clavier virtuel après la recherche.
     */
    private fun triggerSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            hideKeyboard()
            viewModel.searchAll(query)
        }
    }

    /**
     * Cache le clavier virtuel.
     */
    private fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }
}
