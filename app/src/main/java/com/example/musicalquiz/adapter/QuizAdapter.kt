package com.example.musicalquiz.adapter
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicalquiz.R
import com.example.musicalquiz.database.dao.QuizWithPlaylistInfo
import com.example.musicalquiz.database.entities.GameMode
import com.example.musicalquiz.databinding.QuizItemBinding


/**
 * Adapter for displaying a list of quizzes in a RecyclerView
 */
class QuizAdapter(
    private val onQuizClick: (QuizWithPlaylistInfo) -> Unit,
    private val onMenuClick: (QuizWithPlaylistInfo, View) -> Unit
) : ListAdapter<QuizWithPlaylistInfo, QuizAdapter.QuizViewHolder>(QuizDiffCallback()) {


    /**
     * this creates new ViewHolder instances when needed by the RecyclerView
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val binding = QuizItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuizViewHolder(binding)
    }


    /**
     * for  binding at the specified position
     */
    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val quizInfo = getItem(position)
        holder.bind(quizInfo, onQuizClick, onMenuClick)
    }

    override fun submitList(list: List<QuizWithPlaylistInfo>?) {
        super.submitList(list)
    }


    /**
     * viewholder for displaying individual quiz items
     */
    class QuizViewHolder(private val binding: QuizItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            quizInfo: QuizWithPlaylistInfo,
            onQuizClick: (QuizWithPlaylistInfo) -> Unit,
            onMenuClick: (QuizWithPlaylistInfo, View) -> Unit
        ) {
            binding.quizNameTextView.text = quizInfo.quiz.name
            binding.playlistNameTextView.text = itemView.context.getString(R.string.playlist_name_label, quizInfo.playlistName)

            val gameModeString = when (quizInfo.quiz.gameMode) {
                GameMode.MULTIPLE_CHOICE.name -> itemView.context.getString(R.string.game_mode_multiple_choice)
                GameMode.FILL_IN_THE_BLANKS.name -> itemView.context.getString(R.string.game_mode_fill_blanks)
                else -> quizInfo.quiz.gameMode
            }

            val detailsText = "$gameModeString"
            binding.quizDetailsTextView.text = detailsText


            binding.startQuizButton.setOnClickListener {
                onQuizClick(quizInfo)
            }

            binding.menuButton.setOnClickListener { view ->
                onMenuClick(quizInfo, view)
            }

            itemView.setOnClickListener {
                onQuizClick(quizInfo)
            }
        }
    }

    class QuizDiffCallback : DiffUtil.ItemCallback<QuizWithPlaylistInfo>() {
        override fun areItemsTheSame(oldItem: QuizWithPlaylistInfo, newItem: QuizWithPlaylistInfo): Boolean {
            return oldItem.quiz.id == newItem.quiz.id
        }

        override fun areContentsTheSame(oldItem: QuizWithPlaylistInfo, newItem: QuizWithPlaylistInfo): Boolean {
            return oldItem == newItem
        }
    }
}
