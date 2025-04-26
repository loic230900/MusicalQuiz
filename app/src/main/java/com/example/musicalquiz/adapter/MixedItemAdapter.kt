package com.example.musicalquiz.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicalquiz.R
import com.example.musicalquiz.model.Album
import com.example.musicalquiz.model.SearchResultItem
import com.example.musicalquiz.model.Track

/**
 * Adaptateur pour afficher une liste mixte de morceaux et d'albums dans un RecyclerView.
 * Utilise Glide pour charger les images de couverture des albums.
 * Gère l'affichage différencié des morceaux et des albums avec des styles visuels distincts.
 */
class MixedItemAdapter(private var items: List<SearchResultItem>) :
    RecyclerView.Adapter<MixedItemAdapter.MusicViewHolder>() {

    /**
     * ViewHolder pour afficher un élément musical (morceau ou album).
     * Contient les vues nécessaires pour afficher les informations de l'élément.
     */
    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeLabel: TextView = itemView.findViewById(R.id.typeLabel)
        val image: ImageView = itemView.findViewById(R.id.coverImageView)
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val artist: TextView = itemView.findViewById(R.id.artist)
    }

    /**
     * Crée un nouveau ViewHolder pour un élément de la liste.
     * @param parent Le ViewGroup parent dans lequel la nouvelle vue sera ajoutée
     * @param viewType Le type de vue de l'élément
     * @return Un nouveau ViewHolder qui contient une vue pour l'élément
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.track_item, parent, false)
        return MusicViewHolder(view)
    }

    /**
     * Retourne le nombre total d'éléments dans la liste.
     * @return Le nombre d'éléments dans la liste
     */
    override fun getItemCount(): Int = items.size

    /**
     * Lie les données d'un élément à son ViewHolder.
     * Configure l'affichage différencié selon qu'il s'agit d'un morceau ou d'un album.
     * @param holder Le ViewHolder à mettre à jour
     * @param position La position de l'élément dans la liste
     */
    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val item = items[position]

        when (item) {
            is SearchResultItem.TrackItem -> {
                val track: Track = item.track
                holder.typeLabel.setText(R.string.track)
                holder.typeLabel.setBackgroundResource(R.drawable.bg_track_label)
                holder.title.text = track.title
                holder.subtitle.text = track.album.title
                holder.artist.text = track.artist.name
                Glide.with(holder.itemView).load(track.album.cover).into(holder.image)
            }

            is SearchResultItem.AlbumItem -> {
                val album: Album = item.album
                holder.typeLabel.setText(R.string.album)
                holder.typeLabel.setBackgroundResource(R.drawable.bg_album_label)
                holder.title.text = album.title
                holder.subtitle.text = "" // Pas de sous-titre pour un album
                holder.artist.text = album.artist.name
                Glide.with(holder.itemView).load(album.cover).into(holder.image)
            }
        }
    }

    /**
     * Met à jour la liste des éléments de manière optimisée en utilisant DiffUtil.
     * Ne met à jour que les éléments qui ont changé.
     * @param newItems La nouvelle liste d'éléments à afficher
     */
    fun updateData(newItems: List<SearchResultItem>) {
        val diffCallback = MixedItemDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }
}
