package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musicalquiz.R

/**
 * Fragment affichant la liste des quiz créés par l'utilisateur.
 *
 * Chaque quiz peut contenir une sélection personnalisée de pistes musicales,
 * choisies manuellement ou générées aléatoirement. Un quiz peut éventuellement
 * être associé à une playlist, mais ce n’est pas obligatoire.
 *
 * Ce fragment permet également de :
 * - créer un nouveau quiz,
 * - afficher les détails d’un quiz,
 * - lancer un quiz en mode jeu (questions avec aperçu musical).
 */
class QuizFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }
}
