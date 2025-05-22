package com.example.musicalquiz.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicalquiz.R
import com.example.musicalquiz.adapter.QuizAdapter
import com.example.musicalquiz.database.dao.QuizWithPlaylistInfo
import com.example.musicalquiz.database.entities.GameMode
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.database.entities.QuestionSelectionMode
import com.example.musicalquiz.databinding.FragmentQuizBinding
import com.example.musicalquiz.viewmodel.QuizViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.example.musicalquiz.viewmodel.PlaylistViewModel

/**
 * A Fragment for displaying and managing quizzes.
 * Users can view existing quizzes, create new ones and delete them.
 */


class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var quizAdapter: QuizAdapter
    private var availablePlaylists: List<Playlist> = emptyList()
    val MAX_ALLOWED_TIME_LIMIT_SECONDS = 180

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        quizViewModel.loadAvailablePlaylists()
    }

    /**
     * this initializes the RecyclerView with its adapter and layout manager
     * and defines actions for when a quiz item is clicked or its menu is accessed
     */

    private fun setupRecyclerView() {
        quizAdapter = QuizAdapter(
            onQuizClick = { quizInfo ->
                // Navigate to quiz playing screen
                val action = QuizFragmentDirections.actionQuizFragmentToQuizPlayFragment(quizInfo.quiz.id)
                findNavController().navigate(action)
            },
            onMenuClick = { quizInfo, anchorView ->
                showQuizMenu(quizInfo, anchorView)
            }
        )
        binding.quizRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = quizAdapter
        }
    }

    private fun setupObservers() {
        quizViewModel.quizzes.observe(viewLifecycleOwner, Observer { quizzes ->
            quizAdapter.submitList(quizzes)
            updateEmptyState(quizzes.isNullOrEmpty())
        })


        quizViewModel.message.observe(viewLifecycleOwner, Observer { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                quizViewModel.clearMessage() // Clear message after showing
            }
        })

        quizViewModel.availablePlaylists.observe(viewLifecycleOwner, Observer { playlists ->
            this.availablePlaylists = playlists ?: emptyList()
        })
    }

    private fun setupClickListeners() {
        binding.addQuizFab.setOnClickListener {
            showCreateQuizDialog()
        }
    }


    /**
     * Displays a dialog for the user to create a new quiz.
     * The dialog includes fields for quiz name, playlist selection, game mode and number of questions and time limit per question
     */

    private fun showCreateQuizDialog() {
        if (context == null) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_quiz, null)
        val quizNameEditText = dialogView.findViewById<TextInputEditText>(R.id.quizNameEditText)
        val playlistSpinner = dialogView.findViewById<Spinner>(R.id.playlistSpinner)
        val gameModeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.gameModeRadioGroup)
        val numberOfQuestionsEditText = dialogView.findViewById<TextInputEditText>(R.id.numberOfQuestionsEditText)
        val timeLimitEditText = dialogView.findViewById<TextInputEditText>(R.id.timeLimitEditText) //

        val playlistNames = availablePlaylists.map { it.name }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, playlistNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playlistSpinner.adapter = spinnerAdapter

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_create_quiz)
            .setView(dialogView)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_create) { dialog, _ ->
                val quizName = quizNameEditText.text.toString().trim()
                val selectedPlaylistPosition = playlistSpinner.selectedItemPosition

                if (quizName.isBlank()) {
                    Toast.makeText(context, R.string.please_enter_quiz_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (selectedPlaylistPosition < 0 || selectedPlaylistPosition >= availablePlaylists.size) {
                    Toast.makeText(context, R.string.please_select_a_playlist, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val selectedPlaylist = availablePlaylists[selectedPlaylistPosition]

                val selectedGameModeId = gameModeRadioGroup.checkedRadioButtonId
                val gameMode = if (selectedGameModeId == R.id.radioMultipleChoice) GameMode.MULTIPLE_CHOICE
                else GameMode.FILL_IN_THE_BLANKS //

                val numQuestionsString = numberOfQuestionsEditText.text.toString()
                val localNumberOfQuestions = numQuestionsString.toIntOrNull() ?: 10


                val trackCountForSelectedPlaylist = playlistViewModel.playlistTrackCounts.value?.get(selectedPlaylist.id) ?: 0
                if (trackCountForSelectedPlaylist == 0) {
                    Toast.makeText(context, "Selected playlist appears to have no tracks. Please add songs to this playlist or choose a different one.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                var calculatedMaxBasedOnTracks = trackCountForSelectedPlaylist * 3
                val absoluteOverallMax = 30 // ceiling for max number of questions

                val actualMaxAllowedQuestions: Int

                if (trackCountForSelectedPlaylist == 0) { // If playlist has 0 tracks
                    actualMaxAllowedQuestions = 0 // then no questions possible
                } else if (calculatedMaxBasedOnTracks > absoluteOverallMax) {
                    //if the calculated possible questions is more than the ceiling, then the max number is set to the ceiling
                    actualMaxAllowedQuestions = absoluteOverallMax
                } else {

                    //if not, the number of questions possible is number of quesition types (in my case 3)  * (number of songs)
                    actualMaxAllowedQuestions = calculatedMaxBasedOnTracks
                }

                if (actualMaxAllowedQuestions == 0 && localNumberOfQuestions > 0) {
                    Toast.makeText(context, "This playlist has no usable tracks, please add songs to the playlist or choose a different one.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                if (localNumberOfQuestions <= 0 || localNumberOfQuestions > actualMaxAllowedQuestions) {
                    if (actualMaxAllowedQuestions > 0) {
                        Toast.makeText(context, "The max number of questions for this playlist is $actualMaxAllowedQuestions.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Please enter a valid number of questions", Toast.LENGTH_LONG).show()
                    }
                    return@setPositiveButton
                }

                val timeLimitString = timeLimitEditText.text.toString()
                val timeLimitPerQuestion = if (timeLimitString.isNotBlank()) {
                    timeLimitString.toIntOrNull()
                } else {
                    null
                }

                if (timeLimitPerQuestion != null) {
                    if (timeLimitPerQuestion <= 0) {
                        Toast.makeText(context, "Time limit must be a positive number.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (timeLimitPerQuestion > MAX_ALLOWED_TIME_LIMIT_SECONDS) {
                        Toast.makeText(context, "Time limit cannot exceed 3 minutes.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                }


                quizViewModel.createQuiz(
                    name = quizName,
                    playlistId = selectedPlaylist.id,
                    questionSelectionMode = QuestionSelectionMode.RANDOM,
                    gameMode = gameMode,
                    requestedNumberOfQuestions = localNumberOfQuestions,
                    timeLimitPerQuestion = timeLimitPerQuestion
                )
            }
            .show()
    }

    private fun showQuizMenu(quizInfo: QuizWithPlaylistInfo, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.quiz_item_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_quiz -> {
                    showDeleteQuizConfirmationDialog(quizInfo)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteQuizConfirmationDialog(quizInfo: QuizWithPlaylistInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_quiz_confirmation_title)
            .setMessage(getString(R.string.delete_quiz_confirmation_message, quizInfo.quiz.name))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                quizViewModel.deleteQuiz(quizInfo)
            }
            .show()
    }


    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.quizRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
