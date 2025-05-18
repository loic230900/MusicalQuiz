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
import java.io.IOException


/**
 * Fragment responsible for displaying and managing the quiz playing interface
 * It handles question display, answer submission, audio preview playback and showing results
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

    companion object {
        private const val FRAGMENT_TAG = "QuizPlayFragment_DEBUG"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizPlayBinding.inflate(inflater, container, false)
        answerButtons.addAll(listOf(binding.answerButton1, binding.answerButton2, binding.answerButton3, binding.answerButton4))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        quizViewModel.resetQuizState()
        quizViewModel.loadQuestionsForQuiz(args.quizId)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        quizViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(FRAGMENT_TAG, "isLoading LIVEDATA changed to: $isLoading. isQuizFinished: ${quizViewModel.isQuizFinished.value}")
            binding.loadingIndicator.visibility = if (isLoading && quizViewModel.isQuizFinished.value == false) View.VISIBLE else View.GONE
        }

        quizViewModel.currentQuizDetails.observe(viewLifecycleOwner) { quiz ->
            Log.d(FRAGMENT_TAG, "currentQuizDetails LIVEDATA changed. GameMode: ${quiz?.gameMode}")
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
     * Sets up click listeners for interactive UI elements like the play preview button, answer buttons, and submit button.
     */
    private fun setupClickListeners() {
        binding.playPreviewButton.setOnClickListener {
            val currentQuestion = quizViewModel.currentQuestion.value
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.playPreviewButton.setImageResource(R.drawable.ic_play)
                binding.previewStatusTextView.text = "Paused"
            } else if (mediaPlayer != null && currentQuestion?.previewUrl != null) {
                mediaPlayer?.start()
                binding.playPreviewButton.setImageResource(R.drawable.ic_pause)
                binding.previewStatusTextView.text = getString(R.string.playing_preview)
            } else if (currentQuestion?.previewUrl != null) {
                playPreview(currentQuestion.previewUrl)
            }
        }

        answerButtons.forEach { button ->
            button.setOnClickListener {
                val selectedAnswer = (it as Button).text.toString()
                quizViewModel.submitAnswer(selectedAnswer)
                stopPreview()
                disableAnswerInputs()
            }
        }

        binding.submitTextAnswerButton.setOnClickListener {
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

        binding.playPreviewButton.isEnabled = true
        binding.playPreviewButton.setImageResource(R.drawable.ic_play)
        binding.previewStatusTextView.text = ""
    }

    private fun disableAnswerInputs() {
        answerButtons.forEach { it.isEnabled = false }
        binding.answerEditText.isEnabled = false
        binding.submitTextAnswerButton.isEnabled = false
    }

    private fun enableAnswerInputs() {
        answerButtons.forEach { it.isEnabled = true }
        binding.answerEditText.isEnabled = true
        binding.submitTextAnswerButton.isEnabled = true
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

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

    private fun playPreview(url: String) {
        stopPreview()
        binding.playPreviewButton.isEnabled = false
        binding.previewStatusTextView.text = getString(R.string.playing_preview)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(url)
                prepareAsync()
            } catch (e: IOException) {
                e.printStackTrace()
                binding.previewStatusTextView.text = getString(R.string.preview_error)
                binding.playPreviewButton.isEnabled = true
                binding.playPreviewButton.setImageResource(R.drawable.ic_play)
                return@apply
            }

            setOnPreparedListener { mp ->
                mp.start()
                binding.playPreviewButton.setImageResource(R.drawable.ic_pause)
                binding.playPreviewButton.isEnabled = true

                previewRunnable = object : Runnable {
                    override fun run() {
                        if (mp.isPlaying) {
                            val currentPosition = mp.currentPosition / 1000
                            val totalDuration = mp.duration / 1000
                            binding.previewStatusTextView.text = "Playing... $currentPosition / $totalDuration s"
                            handler.postDelayed(this, 1000)
                        }
                    }
                }
                handler.post(previewRunnable!!)
            }

            setOnCompletionListener {
                stopPreview()
            }

            setOnErrorListener { _, _, _ ->
                stopPreview()
                binding.previewStatusTextView.text = getString(R.string.preview_error)
                true
            }
        }
    }

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
        super.onPause()
        stopPreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPreview()
        _binding = null
    }
}
