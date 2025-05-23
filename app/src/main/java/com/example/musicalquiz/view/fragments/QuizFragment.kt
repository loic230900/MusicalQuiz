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
import kotlinx.coroutines.launch

/**
 * Fragment responsible for displaying and managing quizzes in the MusicalQuiz application.
 * This fragment provides:
 * - Display of existing quizzes in a RecyclerView
 * - Creation of new quizzes with customizable settings
 * - Quiz deletion and management
 * - Navigation to quiz gameplay
 * 
 * The fragment supports:
 * - Multiple game modes (Multiple Choice, Fill in the Blanks)
 * - Customizable number of questions
 * - Time limits per question
 * - Playlist selection for quiz content
 * 
 * The fragment uses ViewModels for data management and adapters for displaying
 * quizzes in a RecyclerView. It handles quiz creation validation and ensures
 * the selected playlist has sufficient tracks for the requested number of questions.
 */

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()
    private lateinit var quizAdapter: QuizAdapter
    private var availablePlaylists: List<Playlist> = emptyList()
    val MAX_ALLOWED_TIME_LIMIT_SECONDS = 180

    /**
     * Creates and initializes the fragment's view.
     * Inflates the fragment_quiz layout and returns the root view.
     * 
     * @param inflater LayoutInflater for creating the view
     * @param container Parent view group
     * @param savedInstanceState Saved instance state
     * @return The initialized view
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Initializes the fragment after the view is created.
     * Sets up:
     * - RecyclerView with quiz adapter
     * - ViewModel observers
     * - Click listeners
     * - Available playlists loading
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        quizViewModel.loadAvailablePlaylists()
    }

    /**
     * Initializes the RecyclerView with its adapter and layout manager.
     * Configures:
     * - QuizAdapter with click handlers for quiz selection and menu access
     * - LinearLayoutManager for vertical scrolling
     * - Navigation to quiz gameplay on quiz selection
     * - Quiz menu display on menu button click
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

    /**
     * Sets up observers for ViewModel LiveData objects.
     * Observes:
     * - Quiz list updates
     * - Error messages
     * - Available playlists
     * 
     * Handles:
     * - Quiz list updates with smooth scrolling to new items
     * - Empty state updates
     * - Error message display
     */
    private fun setupObservers() {
        quizViewModel.quizzes.observe(viewLifecycleOwner, Observer { quizzes ->
            val previousSize = quizAdapter.currentList.size
            quizAdapter.submitList(quizzes)
            updateEmptyState(quizzes.isNullOrEmpty())
            
            // If a new quiz was added (list size increased), scroll to show it
            if (quizzes != null && quizzes.size > previousSize) {
                binding.quizRecyclerView.post {
                    binding.quizRecyclerView.smoothScrollToPosition(0)
                }
            }
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

    /**
     * Sets up click listeners for interactive elements.
     * Configures:
     * - Add quiz FAB for creating new quizzes
     */
    private fun setupClickListeners() {
        binding.addQuizFab.setOnClickListener {
            showCreateQuizDialog()
        }
    }

    /**
     * Displays a dialog for creating a new quiz.
     * The dialog includes:
     * - Quiz name input
     * - Playlist selection spinner
     * - Game mode selection (Multiple Choice or Fill in the Blanks)
     * - Number of questions input
     * - Time limit per question input
     * 
     * The dialog validates:
     * - Quiz name is not empty
     * - A playlist is selected
     * - The selected playlist has sufficient tracks
     * - Number of questions is valid for the playlist
     * - Time limit is within allowed range
     */
    private fun showCreateQuizDialog() {
        if (context == null) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_quiz, null)
        val quizNameEditText = dialogView.findViewById<TextInputEditText>(R.id.quizNameEditText)
        val playlistSpinner = dialogView.findViewById<Spinner>(R.id.playlistSpinner)
        val gameModeRadioGroup = dialogView.findViewById<RadioGroup>(R.id.gameModeRadioGroup)
        val numberOfQuestionsEditText = dialogView.findViewById<TextInputEditText>(R.id.numberOfQuestionsEditText)
        val timeLimitEditText = dialogView.findViewById<TextInputEditText>(R.id.timeLimitEditText)

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
                else GameMode.FILL_IN_THE_BLANKS

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
                val timeLimit = timeLimitString.toIntOrNull()

                if (timeLimit != null && (timeLimit <= 0 || timeLimit > MAX_ALLOWED_TIME_LIMIT_SECONDS)) {
                    Toast.makeText(context, "Please enter a valid time limit between 1 and $MAX_ALLOWED_TIME_LIMIT_SECONDS seconds", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    quizViewModel.createQuiz(
                        name = quizName,
                        playlistId = selectedPlaylist.id,
                        questionSelectionMode = QuestionSelectionMode.RANDOM,
                        gameMode = gameMode,
                        timeLimitPerQuestion = timeLimit,
                        requestedNumberOfQuestions = localNumberOfQuestions
                    )
                }
            }
            .show()
    }

    /**
     * Shows a popup menu for quiz management options.
     * The menu includes:
     * - Delete quiz option
     * 
     * @param quizInfo The quiz to manage
     * @param anchorView The view to anchor the popup menu to
     */
    private fun showQuizMenu(quizInfo: QuizWithPlaylistInfo, anchorView: View) {
        PopupMenu(requireContext(), anchorView).apply {
            menuInflater.inflate(R.menu.quiz_item_menu, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog(quizInfo)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    /**
     * Shows a confirmation dialog before deleting a quiz.
     * The dialog includes:
     * - Confirmation message
     * - Cancel and delete buttons
     * 
     * @param quizInfo The quiz to delete
     */
    private fun showDeleteConfirmationDialog(quizInfo: QuizWithPlaylistInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_quiz)
            .setMessage(getString(R.string.delete_quiz_confirmation, quizInfo.quiz.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    quizViewModel.deleteQuiz(quizInfo.quiz.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Updates the UI to show either the empty state or the quiz list.
     * @param isEmpty Whether the quiz list is empty
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.quizRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
