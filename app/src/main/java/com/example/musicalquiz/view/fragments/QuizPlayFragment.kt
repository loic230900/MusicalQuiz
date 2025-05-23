package com.example.musicalquiz.view.fragments

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.musicalquiz.R
import com.example.musicalquiz.database.entities.QuizQuestion
import com.example.musicalquiz.database.entities.GameMode
import com.example.musicalquiz.database.entities.FillBlanksQuestionType
import com.example.musicalquiz.database.entities.MultipleChoiceQuestionFocus
import com.example.musicalquiz.databinding.FragmentQuizPlayBinding
import com.example.musicalquiz.viewmodel.QuizViewModel
import com.google.android.material.button.MaterialButton
import android.os.CountDownTimer
import java.io.IOException


/**
 * Fragment responsible for displaying and managing the quiz playing interface.
 * This fragment provides:
 * - Interactive quiz gameplay with multiple question types
 * - Audio preview playback for tracks
 * - Real-time answer validation
 * - Progress tracking and scoring
 * - Timer functionality for time-limited questions
 * 
 * The fragment supports:
 * - Multiple Choice questions with different focus areas (title, artist, album)
 * - Fill in the Blanks questions with various question types
 * - Time limits per question
 * - Audio preview playback with play/pause controls
 * - Progress tracking and final score display
 * 
 * The fragment uses:
 * - ViewBinding for UI interactions
 * - MediaPlayer for audio preview
 * - CountDownTimer for question time limits
 * - LiveData observers for state management
 * - Navigation component for screen transitions
 */


class QuizPlayFragment : Fragment() {

    private var _binding: FragmentQuizPlayBinding? = null
    private val binding get() = _binding!!

    private val quizViewModel: QuizViewModel by viewModels()
    private val args: QuizPlayFragmentArgs by navArgs()

    private var mediaPlayer: MediaPlayer? = null
    private val answerButtons = mutableListOf<MaterialButton>()

    private val handler = Handler(Looper.getMainLooper())
    private var previewRunnable: Runnable? = null

    private var questionTimer: CountDownTimer? = null
    private var currentQuestionTimeLimit: Int? = null
    private var timeLeftInMillis: Long = 0L
    private val TIMER_UPDATE_INTERVAL = 1000L


    companion object {
        private const val FRAGMENT_TAG = "QuizPlayFragment_DEBUG"
    }

    /**
     * Creates and initializes the fragment's view.
     * Inflates the fragment_quiz_play layout and initializes answer buttons.
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
        _binding = FragmentQuizPlayBinding.inflate(inflater, container, false)
        answerButtons.addAll(listOf(binding.answerButton1, binding.answerButton2, binding.answerButton3, binding.answerButton4))
        return binding.root
    }

    /**
     * Initializes the fragment after the view is created.
     * Sets up:
     * - Quiz state management (new or restored)
     * - ViewModel observers
     * - Click listeners
     * - Question loading
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            quizViewModel.resetQuizState()
            quizViewModel.loadQuestionsForQuiz(args.quizId)
        } else {
            quizViewModel.restoreState()
        }
        setupObservers()
        setupClickListeners()
    }

    /**
     * Saves the current quiz state when the fragment is destroyed.
     * @param outState Bundle to save state to
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        quizViewModel.saveState()
    }

    /**
     * Sets up observers for LiveData from the QuizViewModel.
     * Observes:
     * - Loading state for progress indication
     * - Current quiz details for game mode and time limits
     * - Current question for display and interaction
     * - Question index for progress tracking
     * - Quiz completion status for results display
     * - Error messages for user feedback
     */
    private fun setupObservers() {
        quizViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(FRAGMENT_TAG, "isLoading LIVEDATA changed to: $isLoading. isQuizFinished: ${quizViewModel.isQuizFinished.value}")
            binding.loadingIndicator.visibility = if (isLoading && quizViewModel.isQuizFinished.value == false) View.VISIBLE else View.GONE
        }

        quizViewModel.currentQuizDetails.observe(viewLifecycleOwner) { quiz ->
            Log.d(FRAGMENT_TAG, "currentQuizDetails LIVEDATA changed. GameMode: ${quiz?.gameMode}")
            currentQuestionTimeLimit = quiz?.timeLimitPerQuestion
            if (quizViewModel.currentQuestion.value != null && quizViewModel.isQuizFinished.value == false) {
                quizViewModel.currentQuestion.value?.let { displayQuestion(it) }
            }
        }

        quizViewModel.currentQuestion.observe(viewLifecycleOwner) { question ->
            if (quizViewModel.isQuizFinished.value == false) {
                question?.let {
                    Log.d(FRAGMENT_TAG, "CurrentQuestion LIVEDATA: Displaying question: ${it.correctAnswer}, MC Focus: ${it.mcQuestionFocus}")
                    displayQuestion(it)
                }
            }
        }

        quizViewModel.currentQuestionIndex.observe(viewLifecycleOwner) { index ->
            Log.d(FRAGMENT_TAG, "currentQuestionIndex LIVEDATA changed to: $index")
            updateProgress()
        }

        quizViewModel.isQuizFinished.observe(viewLifecycleOwner) { isFinished ->
            Log.d(FRAGMENT_TAG, "isQuizFinished LIVEDATA changed to: $isFinished")
            if (isFinished) {
                Log.d(FRAGMENT_TAG, "Quiz is finished, calling showResults().")
                binding.timerTextView.visibility = View.GONE
                showResults()
            } else {
                Log.d(FRAGMENT_TAG, "Quiz is NOT finished, ensuring quiz UI is visible.")
                binding.resultsLayout.visibility = View.GONE
            }
        }

        quizViewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.d(FRAGMENT_TAG, "Message LIVEDATA: $it")
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                quizViewModel.clearMessage()
            }
        }
    }


    /**
     * Sets up click listeners for interactive UI elements.
     * Configures:
     * - Play/pause preview button for audio playback
     * - Answer buttons for multiple choice questions
     * - Submit button for fill in the blanks questions
     * - Back to quizzes button for navigation
     * 
     * Also handles:
     * - Keyboard actions for text input
     * - Preview playback state management
     * - Answer submission and validation
     */
    private fun setupClickListeners() {
        binding.playPreviewButton.setOnClickListener {
            val currentQuestion = quizViewModel.currentQuestion.value
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.playPreviewButton.setImageResource(R.drawable.ic_play)
                binding.previewStatusTextView.text = getString(R.string.preview_paused)
            } else if (mediaPlayer != null && currentQuestion?.previewUrl != null) {
                try {
                    mediaPlayer?.start()
                    binding.playPreviewButton.setImageResource(R.drawable.ic_pause)
                    binding.previewStatusTextView.text = getString(R.string.playing_preview)
                } catch (e: Exception) {
                    Log.e(FRAGMENT_TAG, "Error resuming preview: ${e.message}", e)
                    binding.previewStatusTextView.text = getString(R.string.preview_error)
                    stopPreview()
                }
            } else if (currentQuestion?.previewUrl != null) {
                playPreview(currentQuestion.previewUrl)
            } else {
                binding.previewStatusTextView.text = getString(R.string.no_preview_available)
            }
        }

        answerButtons.forEach { button ->
            button.setOnClickListener {
                questionTimer?.cancel()
                val selectedAnswer = (it as Button).text.toString()
                quizViewModel.submitAnswer(selectedAnswer)
                stopPreview()
                disableAnswerInputs()
            }
        }

        binding.submitTextAnswerButton.setOnClickListener {
            questionTimer?.cancel()
            val typedAnswer = binding.answerEditText.text.toString().trim()
            if (typedAnswer.isNotEmpty()) {
                quizViewModel.submitAnswer(typedAnswer)
                stopPreview()
                binding.answerEditText.text?.clear()
                hideKeyboard()
                disableAnswerInputs()
            } else {
                Toast.makeText(context, "Please type an answer", Toast.LENGTH_SHORT).show()
            }
        }
        binding.answerEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.submitTextAnswerButton.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.backToQuizzesButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * Displays a quiz question and configures the UI based on the question type.
     * Handles:
     * - Multiple choice questions with answer buttons
     * - Fill in the blanks questions with text input
     * - Question text and instructions
     * - Preview playback controls
     * - Timer initialization
     * 
     * @param question The question to display
     */
    private fun displayQuestion(question: QuizQuestion) {
        updateProgress()
        binding.answerEditText.text?.clear()
        enableAnswerInputs()

        val gameModeString = quizViewModel.currentQuizDetails.value?.gameMode
        Log.d(FRAGMENT_TAG, "displayQuestion - GameMode from ViewModel: $gameModeString, MC Focus: ${question.mcQuestionFocus}")

        if (gameModeString == GameMode.MULTIPLE_CHOICE.name) {
            binding.answerButtonsContainer.visibility = View.VISIBLE
            binding.fillBlanksInputContainer.visibility = View.GONE

            val promptText = when (question.mcQuestionFocus) {
                MultipleChoiceQuestionFocus.TRACK_TITLE.name -> getString(R.string.guess_the_song_prompt)
                MultipleChoiceQuestionFocus.ARTIST_NAME.name -> getString(R.string.guess_the_artist_prompt)
                MultipleChoiceQuestionFocus.ALBUM_TITLE.name -> getString(R.string.guess_the_album_prompt)
                else -> {
                    Log.w(FRAGMENT_TAG, "Unknown or null mcQuestionFocus: ${question.mcQuestionFocus}, defaulting to guess song.")
                    getString(R.string.guess_the_song_prompt)
                }
            }
            binding.questionPromptTextView.text = promptText
            Log.d(FRAGMENT_TAG, "MC Prompt set to: $promptText")

            val options = mutableListOf(question.correctAnswer)
            question.incorrectOption1?.let { options.add(it) }
            question.incorrectOption2?.let { options.add(it) }
            question.incorrectOption3?.let { options.add(it) }
            options.shuffle()

            answerButtons.forEachIndexed { index, button ->
                if (index < options.size) {
                    button.text = options[index]
                    button.visibility = View.VISIBLE
                } else {
                    button.visibility = View.GONE
                }
            }
        } else if (gameModeString == GameMode.FILL_IN_THE_BLANKS.name) {
            binding.answerButtonsContainer.visibility = View.GONE
            binding.fillBlanksInputContainer.visibility = View.VISIBLE
            val promptPrefix = when (question.fillBlanksQuestionType) {
                FillBlanksQuestionType.TRACK_TITLE.name -> getString(R.string.question_prompt_fill_blanks_title)
                FillBlanksQuestionType.ARTIST_NAME.name -> getString(R.string.question_prompt_fill_blanks_artist)
                FillBlanksQuestionType.ALBUM_TITLE.name -> getString(R.string.question_prompt_fill_blanks_album)
                else -> {
                    Log.w(FRAGMENT_TAG, "Unknown or null fillBlanksQuestionType: ${question.fillBlanksQuestionType}, defaulting to guess song.")
                    getString(R.string.guess_the_song_prompt)
                }
            }
            binding.questionPromptTextView.text = "$promptPrefix\n${question.fillBlanksPrompt ?: question.correctAnswer}"
            binding.answerEditText.requestFocus()

        } else {
            binding.answerButtonsContainer.visibility = View.GONE
            binding.fillBlanksInputContainer.visibility = View.GONE
            binding.questionPromptTextView.text = getString(R.string.game_mode_not_supported)
            Log.e(FRAGMENT_TAG, "Unsupported game mode in displayQuestion: $gameModeString")
        }


        /* triggering the timer for the questions if applicable*/
        currentQuestionTimeLimit?.let { limitInSeconds ->
            if (limitInSeconds > 0) {
                binding.timerContainer.visibility = View.VISIBLE // shows the timer UI only if the user choose to play with time limit
                startQuestionTimer(limitInSeconds)
            } else {
                binding.timerContainer.visibility = View.GONE
                questionTimer?.cancel()
            }
        } ?: run {
            binding.timerContainer.visibility = View.GONE
            questionTimer?.cancel()
        }

        binding.playPreviewButton.isEnabled = true
        binding.playPreviewButton.setImageResource(R.drawable.ic_play)
        binding.previewStatusTextView.text = ""
    }

    /**
     * Initializes and starts the question timer.
     * The timer:
     * - Updates the UI every second
     * - Automatically submits the answer when time runs out
     * - Can be cancelled when an answer is submitted
     * 
     * @param timeLimitSeconds The time limit in seconds
     */
    private fun startQuestionTimer(timeLimitSeconds: Int) {
        questionTimer?.cancel()
        timeLeftInMillis = timeLimitSeconds * 1000L

        binding.circularTimerProgress.max = timeLimitSeconds
        binding.circularTimerProgress.progress = timeLimitSeconds

        questionTimer = object : CountDownTimer(timeLeftInMillis, TIMER_UPDATE_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val secondsLeft = (millisUntilFinished / 1000)
                binding.timerTextView.text = (secondsLeft + 1).toString() //displaying it as a countdown
                binding.circularTimerProgress.setProgressCompat(secondsLeft.toInt() + 1, true)
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                binding.timerTextView.text = "Your Time's Up! "
                binding.circularTimerProgress.setProgressCompat(0, true)

                if (isAdded && view != null) {
                    Toast.makeText(context, getString(R.string.time_up), Toast.LENGTH_SHORT).show()
                    stopPreview()
                    disableAnswerInputs()
                    quizViewModel.submitAnswer("")
                }
            }
        }.start()
        binding.timerContainer.visibility = View.VISIBLE
    }


    /**
     * Enables user input for answering questions.
     * Shows appropriate UI elements based on game mode.
     */
    private fun enableAnswerInputs() {
        answerButtons.forEach { it.isEnabled = true }
        binding.answerEditText.isEnabled = true
        binding.submitTextAnswerButton.isEnabled = true
    }

    /**
     * Disables all answer input methods after an answer is submitted.
     * This includes:
     * - Multiple choice answer buttons
     * - Fill in the blanks text input
     * - Submit button
     */
    private fun disableAnswerInputs() {
        answerButtons.forEach { it.isEnabled = false }
        binding.answerEditText.isEnabled = false
        binding.submitTextAnswerButton.isEnabled = false
    }

    /**
     * Hides the software keyboard.
     * Used after submitting a text answer.
     */
    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    /**
     * Updates the progress indicator to show the current question number
     * and total questions.
     */
    private fun updateProgress() {
        val totalQuestions = quizViewModel.currentQuizQuestions.value?.size ?: 0
        val currentIndex = quizViewModel.currentQuestionIndex.value ?: 0
        if (totalQuestions > 0) {
            binding.questionNumberTextView.text = getString(R.string.question_progress_format, currentIndex + 1, totalQuestions)
            binding.quizProgressIndicator.max = totalQuestions
            binding.quizProgressIndicator.progress = currentIndex + 1
        } else {
            binding.questionNumberTextView.text = ""
            binding.quizProgressIndicator.progress = 0
        }
    }

    /**
     * Plays the audio preview for the current question.
     * Handles:
     * - MediaPlayer initialization
     * - Audio stream configuration
     * - Error handling
     * - UI state updates
     * 
     * @param previewUrl The URL of the audio preview
     */
    private fun playPreview(previewUrl: String) {
        stopPreview()
        binding.playPreviewButton.isEnabled = false
        binding.previewStatusTextView.text = getString(R.string.playing_preview)

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(previewUrl)
                prepareAsync()
                setOnPreparedListener {
                    try {
                        start()
                        binding.playPreviewButton.setImageResource(R.drawable.ic_pause)
                        binding.playPreviewButton.isEnabled = true

                        previewRunnable = object : Runnable {
                            override fun run() {
                                if (isPlaying) {
                                    val currentPosition = currentPosition / 1000
                                    val totalDuration = duration / 1000
                                    binding.previewStatusTextView.text = "Playing... $currentPosition / $totalDuration s"
                                    handler.postDelayed(this, 1000)
                                }
                            }
                        }
                        handler.post(previewRunnable!!)
                    } catch (e: Exception) {
                        Log.e(FRAGMENT_TAG, "Error starting preview: ${e.message}", e)
                        binding.previewStatusTextView.text = getString(R.string.preview_error)
                        binding.playPreviewButton.isEnabled = true
                        binding.playPreviewButton.setImageResource(R.drawable.ic_play)
                    }
                }
                setOnCompletionListener {
                    stopPreview()
                }
                setOnErrorListener { _, what, extra ->
                    val errorMessage = when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> "Unknown error"
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Server died"
                        else -> "Error code: $what, Extra: $extra"
                    }
                    Log.e(FRAGMENT_TAG, "MediaPlayer error: $errorMessage")
                    stopPreview()
                    binding.previewStatusTextView.text = getString(R.string.preview_error)
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(FRAGMENT_TAG, "Error initializing preview: ${e.message}", e)
            binding.previewStatusTextView.text = getString(R.string.preview_error)
            binding.playPreviewButton.isEnabled = true
            binding.playPreviewButton.setImageResource(R.drawable.ic_play)
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    /**
     * Stops the current audio preview playback.
     * Handles:
     * - MediaPlayer cleanup
     * - UI state updates
     * - Preview status text updates
     */
    private fun stopPreview() {
        previewRunnable?.let { handler.removeCallbacks(it) }
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        binding.playPreviewButton.setImageResource(R.drawable.ic_play)
        binding.playPreviewButton.isEnabled = true
        val currentStatus = binding.previewStatusTextView.text.toString()
        if (currentStatus.startsWith("Playing") || currentStatus.startsWith("Paused")) {
            binding.previewStatusTextView.text = ""
        }
    }

    /**
     * Displays the quiz results screen showing:
     * - Final score
     * - Number of correct answers
     * - Total questions
     * - Navigation options
     */
    private fun showResults() {
        stopPreview()
        hideKeyboard()
        binding.questionNumberTextView.visibility = View.GONE
        binding.questionPromptTextView.visibility = View.GONE
        binding.playPreviewButton.visibility = View.GONE
        binding.previewStatusTextView.visibility = View.GONE
        binding.answerButtonsContainer.visibility = View.GONE
        binding.fillBlanksInputContainer.visibility = View.GONE
        binding.quizProgressIndicator.visibility = View.GONE
        binding.loadingIndicator.visibility = View.GONE

        binding.resultsLayout.visibility = View.VISIBLE
        val score = quizViewModel.score.value ?: 0
        val totalQuestions = quizViewModel.currentQuizQuestions.value?.size ?: 0
        binding.scoreTextView.text = getString(R.string.score_format, score, totalQuestions)
    }

    override fun onPause() {
        questionTimer?.cancel()
        super.onPause()
        stopPreview()
        quizViewModel.saveState()
    }

    /**
     * Cleans up resources when the fragment is destroyed.
     * Handles:
     * - MediaPlayer release
     * - Timer cancellation
     * - Handler cleanup
     * - View binding cleanup
     */
    override fun onDestroyView() {
        super.onDestroyView()
        stopPreview()
        questionTimer?.cancel()
        previewRunnable?.let { handler.removeCallbacks(it) }
        _binding = null
    }
}
