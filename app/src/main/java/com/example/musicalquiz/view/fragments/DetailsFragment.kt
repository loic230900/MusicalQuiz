package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musicalquiz.R

/**
 * Fragment affichant les détails d'un morceau ou d'un album.
 * Inclura un aperçu audio si l'élément est une piste.
 */
class DetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }
}
