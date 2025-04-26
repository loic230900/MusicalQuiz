package com.example.musicalquiz.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.musicalquiz.model.SearchResultItem
import com.example.musicalquiz.model.getUniqueId

class MixedItemDiffCallback(
    private val oldList: List<SearchResultItem>,
    private val newList: List<SearchResultItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].getUniqueId() == newList[newItemPosition].getUniqueId()
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
