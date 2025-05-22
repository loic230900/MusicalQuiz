package com.example.musicalquiz.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicalquiz.database.AppDatabase
import com.example.musicalquiz.database.dao.QuizWithPlaylistInfo
import com.example.musicalquiz.database.entities.Playlist
import com.example.musicalquiz.database.entities.Quiz
import com.example.musicalquiz.database.entities.QuizQuestion
import com.example.musicalquiz.database.entities.GameMode
import com.example.musicalquiz.database.entities.QuestionSelectionMode
import com.example.musicalquiz.database.entities.FillBlanksQuestionType
import com.example.musicalquiz.database.entities.MultipleChoiceQuestionFocus
import com.example.musicalquiz.model.Track
import com.example.musicalquiz.network.RetrofitInstance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random



/**
 * ViewModel for managing quiz-related data and operations in the MusicalQuiz application.
 * 
 * This class handles:
 * - Creating and managing quizzes
 * - Loading and managing quiz questions
 * - Tracking quiz state (current question, score)
 * - Interacting with the database for quiz persistence
 * - Managing different game modes (Multiple Choice, Fill in the Blanks)
 * - Handling quiz state restoration
 * 
 * The ViewModel uses LiveData to notify observers of state changes and maintains
 * the current state of active quizzes.
 */
class QuizViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "QuizViewModel_DEBUG"
        private const val KEY_CURRENT_QUIZ_ID = "current_quiz_id"
        private const val KEY_CURRENT_QUESTION_INDEX = "current_question_index"
        private const val KEY_SCORE = "score"
        private const val KEY_IS_QUIZ_FINISHED = "is_quiz_finished"
    }

    private val database = AppDatabase.getDatabase(application)
    private val quizDao = database.quizDao()
    private val quizQuestionDao = database.quizQuestionDao()
    private val playlistDao = database.playlistDao()
    private val playlistTrackDao = database.playlistTrackDao()
    private val deezerApi = RetrofitInstance.api

    // --- LiveData properties ---
    private val _quizzes = MutableLiveData<List<QuizWithPlaylistInfo>>(emptyList())
    val quizzes: LiveData<List<QuizWithPlaylistInfo>> = _quizzes
    private val _availablePlaylists = MutableLiveData<List<Playlist>>(emptyList())
    val availablePlaylists: LiveData<List<Playlist>> = _availablePlaylists
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    private val _currentQuizDetails = MutableLiveData<Quiz?>()
    val currentQuizDetails: LiveData<Quiz?> = _currentQuizDetails
    private val _currentQuizQuestions = MutableLiveData<List<QuizQuestion>>()
    val currentQuizQuestions: LiveData<List<QuizQuestion>> = _currentQuizQuestions
    private val _currentQuestion = MutableLiveData<QuizQuestion?>()
    val currentQuestion: LiveData<QuizQuestion?> = _currentQuestion
    private val _currentQuestionIndex = MutableLiveData(0)
    val currentQuestionIndex: LiveData<Int> = _currentQuestionIndex
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score
    private val _isQuizFinished = MutableLiveData(false)
    val isQuizFinished: LiveData<Boolean> = _isQuizFinished

    init {
        loadAllQuizzes()
        loadAvailablePlaylists()
    }

    /**
     * Saves the current state of the quiz.
     * This includes:
     * - Current quiz ID
     * - Current question index
     * - Current score
     * - Quiz completion status
     * 
     * Used for state restoration when the activity is recreated.
     */
    fun saveState() {
        val currentQuiz = _currentQuizDetails.value
        if (currentQuiz != null) {
            _currentQuizId = currentQuiz.id
            _currentQuestionIndex.value?.let { _savedQuestionIndex = it }
            _score.value?.let { _savedScore = it }
            _isQuizFinished.value?.let { _savedIsQuizFinished = it }
        }
    }

    /**
     * Restores the previously saved quiz state.
     * This method:
     * 1. Loads the questions for the saved quiz
     * 2. Restores the question index
     * 3. Restores the score
     * 4. Restores the quiz completion status
     */
    fun restoreState() {
        if (_currentQuizId != null) {
            loadQuestionsForQuiz(_currentQuizId!!)
            _currentQuestionIndex.value = _savedQuestionIndex
            _score.value = _savedScore
            _isQuizFinished.value = _savedIsQuizFinished
        }
    }

    private var _currentQuizId: Int? = null
    private var _savedQuestionIndex: Int = 0
    private var _savedScore: Int = 0
    private var _savedIsQuizFinished: Boolean = false

    /**
     * Creates a new quiz with the specified parameters.
     * This method:
     * 1. Fetches tracks from the selected playlist
     * 2. Generates appropriate questions based on the game mode
     * 3. Saves the quiz and its questions to the database
     * 
     * @param name The name of the quiz
     * @param playlistId The ID of the playlist to use for questions
     * @param questionSelectionMode How questions should be selected
     * @param gameMode The type of quiz (Multiple Choice or Fill in the Blanks)
     * @param requestedNumberOfQuestions The desired number of questions (default: 10)
     */
    fun createQuiz(
        name: String,
        playlistId: Int,
        questionSelectionMode: QuestionSelectionMode,
        gameMode: GameMode,
        requestedNumberOfQuestions: Int = 10
    ) {
        viewModelScope.launch {
            Log.d(TAG, "createQuiz called. Name: $name, PlaylistID: $playlistId, GameMode: ${gameMode.name}, Requested Questions: $requestedNumberOfQuestions")
            _isLoading.postValue(true)
            _message.postValue(null)
            try {
                val playlistTracks = playlistTrackDao.getTracksForPlaylist(playlistId).firstOrNull()
                if (playlistTracks.isNullOrEmpty()) {
                    Log.e(TAG, "Playlist $playlistId has no tracks.")
                    _message.postValue("Selected playlist has no tracks.")
                    _isLoading.postValue(false)
                    return@launch
                }
                Log.d(TAG, "Found ${playlistTracks.size} tracks in playlist $playlistId.")

                val allFetchedTracksFromPlaylist = mutableListOf<Track>()
                Log.d(TAG, "Fetching details for all ${playlistTracks.size} tracks from Deezer to build question pool...")
                for (pt in playlistTracks) {
                    try {
                        val response = deezerApi.getTrack(pt.trackId)
                        if (response.isSuccessful) {
                            response.body()?.let { track ->
                                if (!track.preview.isNullOrBlank() && track.title.isNotBlank() && track.artist.name.isNotBlank() && track.album.title.isNotBlank()) {
                                    allFetchedTracksFromPlaylist.add(track)
                                } else {
                                    Log.w(TAG, "Skipping track ${track.id} (from playlist) due to missing essential details.")
                                }
                            }
                        } else {
                            Log.e(TAG, "API Error for track ${pt.trackId}: ${response.code()} - ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception fetching details for track ${pt.trackId}: ${e.message}", e)
                    }
                }

                if (allFetchedTracksFromPlaylist.isEmpty()) {
                    Log.e(TAG, "Could not fetch ANY valid track details for questions from playlist $playlistId.")
                    _message.postValue("Could not fetch valid track details for any question from this playlist.")
                    _isLoading.postValue(false)
                    return@launch
                }
                Log.d(TAG, "Successfully fetched details for ${allFetchedTracksFromPlaylist.size} tracks to form question pool.")

                val potentialQuizQuestions = mutableListOf<QuizQuestion>()
                allFetchedTracksFromPlaylist.forEach { track ->
                    val cleanTrackTitle = track.title.trim()
                    val cleanArtistName = track.artist.name.trim()
                    val cleanAlbumTitle = track.album.title.trim()

                    Log.d(TAG, "Processing track: $cleanTrackTitle by $cleanArtistName")
                    val questionsBefore = potentialQuizQuestions.size

                    if (gameMode == GameMode.MULTIPLE_CHOICE) {
                        potentialQuizQuestions.add(QuizQuestion(quizId = 0, trackId = track.id, previewUrl = track.preview!!, correctAnswer = cleanTrackTitle, trackTitle = cleanTrackTitle, artistName = cleanArtistName, albumTitle = cleanAlbumTitle, mcQuestionFocus = MultipleChoiceQuestionFocus.TRACK_TITLE.name))
                        if (cleanArtistName.isNotBlank()) { potentialQuizQuestions.add(QuizQuestion(quizId = 0, trackId = track.id, previewUrl = track.preview, correctAnswer = cleanArtistName, trackTitle = cleanTrackTitle, artistName = cleanArtistName, albumTitle = cleanAlbumTitle, mcQuestionFocus = MultipleChoiceQuestionFocus.ARTIST_NAME.name)) }
                        if (cleanAlbumTitle.isNotBlank()) { potentialQuizQuestions.add(QuizQuestion(quizId = 0, trackId = track.id, previewUrl = track.preview, correctAnswer = cleanAlbumTitle, trackTitle = cleanTrackTitle, artistName = cleanArtistName, albumTitle = cleanAlbumTitle, mcQuestionFocus = MultipleChoiceQuestionFocus.ALBUM_TITLE.name)) }
                    } else if (gameMode == GameMode.FILL_IN_THE_BLANKS) {
                        val questionType = FillBlanksQuestionType.entries.toTypedArray().random()
                        var actualCorrectAnswer = ""
                        var prompt = ""

                        when (questionType) {
                            FillBlanksQuestionType.TRACK_TITLE -> { actualCorrectAnswer = cleanTrackTitle; prompt = generateBlanks(actualCorrectAnswer) }
                            FillBlanksQuestionType.ARTIST_NAME -> { actualCorrectAnswer = cleanArtistName; prompt = generateBlanks(actualCorrectAnswer) }
                            FillBlanksQuestionType.ALBUM_TITLE -> { actualCorrectAnswer = cleanAlbumTitle; prompt = generateBlanks(actualCorrectAnswer) }
                        }
                        if (prompt.isNotBlank() && actualCorrectAnswer.isNotBlank()) {
                            potentialQuizQuestions.add(QuizQuestion(quizId = 0, trackId = track.id, previewUrl = track.preview!!, correctAnswer = actualCorrectAnswer, trackTitle = cleanTrackTitle, artistName = cleanArtistName, albumTitle = cleanAlbumTitle, fillBlanksQuestionType = questionType.name, fillBlanksPrompt = prompt))
                        }
                    }
                    Log.d(TAG, "Added ${potentialQuizQuestions.size - questionsBefore} questions for track $cleanTrackTitle")
                }

                if (potentialQuizQuestions.isEmpty()) {
                    Log.e(TAG, "No potential questions could be generated for game mode ${gameMode.name}.")
                    _message.postValue("Could not generate any questions for this quiz setup.")
                    _isLoading.postValue(false)
                    return@launch
                }

                Log.d(TAG, "Generated ${potentialQuizQuestions.size} potential questions before shuffling")
                potentialQuizQuestions.shuffle(Random.Default)
                val finalQuestionsForQuiz = potentialQuizQuestions.take(requestedNumberOfQuestions.coerceAtMost(potentialQuizQuestions.size))
                Log.d(TAG, "Selected ${finalQuestionsForQuiz.size} questions from ${potentialQuizQuestions.size} potential questions (requested: $requestedNumberOfQuestions)")

                if (finalQuestionsForQuiz.isEmpty()) {
                    Log.e(TAG, "After selection, no questions remained for the quiz.")
                    _message.postValue("Not enough unique questions could be formed.")
                    _isLoading.postValue(false)
                    return@launch
                }
                Log.d(TAG, "Selected ${finalQuestionsForQuiz.size} questions for the quiz from a pool of ${potentialQuizQuestions.size}.")

                val newQuiz = Quiz(name = name, playlistId = playlistId, gameMode = gameMode.name)
                val insertedQuizId = withContext(Dispatchers.IO) { quizDao.insertQuiz(newQuiz) }.toInt()
                Log.d(TAG, "Inserted Quiz with ID: $insertedQuizId")

                if (insertedQuizId <= 0) {
                    Log.e(TAG, "Failed to create quiz entry in database.")
                    _message.postValue("Failed to create quiz entry in database.")
                    _isLoading.postValue(false)
                    return@launch
                }

                val questionsToSaveInDb = finalQuestionsForQuiz.mapIndexed { index, q ->
                    var updatedQ = q.copy(quizId = insertedQuizId, displayOrder = index)
                    if (gameMode == GameMode.MULTIPLE_CHOICE && updatedQ.mcQuestionFocus != null) {
                        try {
                            val focusType = MultipleChoiceQuestionFocus.valueOf(updatedQ.mcQuestionFocus!!)
                            val incorrect = generateIncorrectMcAnswers(updatedQ, allFetchedTracksFromPlaylist, focusType)
                            updatedQ = updatedQ.copy(
                                incorrectOption1 = incorrect.getOrNull(0),
                                incorrectOption2 = incorrect.getOrNull(1),
                                incorrectOption3 = incorrect.getOrNull(2)
                            )
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, "Invalid mcQuestionFocus string: ${updatedQ.mcQuestionFocus}", e)
                        }
                    }
                    updatedQ
                }

                withContext(Dispatchers.IO) { quizQuestionDao.insertAllQuestions(questionsToSaveInDb) }
                Log.i(TAG, "Quiz '$name' (ID: $insertedQuizId) created successfully with ${questionsToSaveInDb.size} questions!")
                _message.postValue("Quiz '$name' created successfully with ${questionsToSaveInDb.size} questions!")
                loadAllQuizzes()

            } catch (e: Exception) {
                Log.e(TAG, "Error creating quiz: ${e.message}", e)
                _message.postValue("Error creating quiz: ${e.message}")
            } finally {
                _isLoading.postValue(false)
                Log.d(TAG, "createQuiz finished, isLoading set to false.")
            }
        }
    }

    private fun generateBlanks(text: String, revealPercentage: Double = 0.4): String {
        if (text.isBlank()) return ""
        val chars = text.toCharArray()
        val numToBlank = (text.length * (1.0 - revealPercentage)).toInt().coerceIn(1, text.length -1)
        if (numToBlank <= 0 && text.length > 1) return text
        val blankableIndices = text.indices.filter { chars[it].isLetterOrDigit() }.toMutableList()
        blankableIndices.shuffle(Random.Default)
        val blankedIndices = blankableIndices.take(numToBlank).toSet()
        val result = StringBuilder()
        for (i in chars.indices) {
            if (blankedIndices.contains(i)) { result.append('_') } else { result.append(chars[i]) }
        }
        return result.toString().trim()
    }

    private fun generateIncorrectMcAnswers(
        currentQuestion: QuizQuestion,
        allTracksPool: List<Track>,
        focus: MultipleChoiceQuestionFocus
    ): List<String> {
        val correctAnswerText = currentQuestion.correctAnswer.trim()
        val potentialDistractors = mutableSetOf<String>()

        allTracksPool.forEach { track ->
            val candidate: String? = when (focus) {
                MultipleChoiceQuestionFocus.TRACK_TITLE -> track.title.trim()
                MultipleChoiceQuestionFocus.ARTIST_NAME -> track.artist.name.trim()
                MultipleChoiceQuestionFocus.ALBUM_TITLE -> track.album.title.trim()
            }
            if (candidate != null && candidate.isNotBlank() && !candidate.equals(correctAnswerText, ignoreCase = true)) {
                potentialDistractors.add(candidate)
            }
        }

        // Remove any duplicates and the correct answer
        potentialDistractors.removeIf { it.equals(correctAnswerText, ignoreCase = true) }
        return potentialDistractors.shuffled(Random.Default).take(3).toList()
    }

    fun loadQuestionsForQuiz(quizId: Int) {
        viewModelScope.launch {
            Log.d(TAG, "loadQuestionsForQuiz called for quizId: $quizId")
            _isLoading.postValue(true)
            _currentQuizDetails.postValue(null)
            _currentQuizQuestions.postValue(emptyList())

            val currentQuiz = withContext(Dispatchers.IO) { quizDao.getQuizById(quizId) }
            if (currentQuiz == null) {
                Log.e(TAG, "Quiz with ID $quizId not found in DB.")
                _message.postValue("Quiz not found.")
                _isQuizFinished.postValue(true)
                _isLoading.postValue(false)
                return@launch
            }
            _currentQuizDetails.postValue(currentQuiz)
            Log.d(TAG, "Current quiz details loaded: ${currentQuiz.name}, GameMode: ${currentQuiz.gameMode}")

            val questionsFromDb = try {
                withContext(Dispatchers.IO) {
                    quizQuestionDao.getQuestionsForQuiz(quizId).firstOrNull()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching questions for quizId $quizId from DAO: ${e.message}", e)
                null
            }

            if (questionsFromDb.isNullOrEmpty()) {
                Log.e(TAG, "No questions found in DB for quizId: $quizId. DAO returned null or empty.")
                _message.postValue("No questions found for this quiz.")
                _currentQuestion.postValue(null)
                _isQuizFinished.postValue(true)
            } else {
                Log.d(TAG, "Loaded ${questionsFromDb.size} questions for quizId: $quizId")
                _currentQuizQuestions.postValue(questionsFromDb)
                _currentQuestionIndex.postValue(0)
                _score.postValue(0)
                _currentQuestion.postValue(questionsFromDb[0])
                Log.d(TAG, "Posted first question: ${questionsFromDb[0].correctAnswer} (Prompt: ${questionsFromDb[0].fillBlanksPrompt}) (MC Focus: ${questionsFromDb[0].mcQuestionFocus})")
                _isQuizFinished.postValue(false)
            }
            _isLoading.postValue(false)
            Log.d(TAG, "loadQuestionsForQuiz finished for quizId $quizId, isLoading set to false.")
        }
    }

    private fun updateCurrentQuestion() {
        val questions = _currentQuizQuestions.value
        val index = _currentQuestionIndex.value
        Log.d(TAG, "updateCurrentQuestion called. Index: $index, Questions loaded: ${questions?.size}")

        if (questions != null && index != null && index < questions.size) {
            _currentQuestion.postValue(questions[index])
            Log.d(TAG, "Posting question at index $index: ${questions[index].correctAnswer} (Prompt: ${questions[index].fillBlanksPrompt}) (MC Focus: ${questions[index].mcQuestionFocus})")
        } else {
            Log.d(TAG, "No more questions or invalid state. Finishing quiz. questions is null: ${questions == null}, index is null: ${index == null}, index value: $index, questions.size: ${questions?.size}")
            _currentQuestion.postValue(null)
            _isQuizFinished.postValue(true)
        }
    }

    fun submitAnswer(userInput: String) {
        val currentQ = _currentQuestion.value ?: return
        val currentQuiz = _currentQuizDetails.value ?: return

        Log.d(TAG, "submitAnswer called. Correct Full Answer: '${currentQ.correctAnswer}', User Input: '$userInput', GameMode: ${currentQuiz.gameMode}")

        var isCorrect = false
        if (currentQuiz.gameMode == GameMode.FILL_IN_THE_BLANKS.name) {
            val prompt = currentQ.fillBlanksPrompt
            val fullCorrectAnswer = currentQ.correctAnswer
            if (prompt != null && prompt.length == fullCorrectAnswer.length) {
                val expectedMissingLetters = StringBuilder()
                for (i in prompt.indices) {
                    if (prompt[i] == '_' && fullCorrectAnswer[i] != '_') {
                        expectedMissingLetters.append(fullCorrectAnswer[i])
                    }
                }
                Log.d(TAG, "FillBlanks Check: Prompt='${prompt}', FullCorrect='${fullCorrectAnswer}', ExpectedMissing='${expectedMissingLetters}', UserInput='${userInput}'")
                isCorrect = userInput.equals(expectedMissingLetters.toString(), ignoreCase = true)
            } else {
                Log.e(TAG, "FillBlanks check issue: Prompt or correctAnswer mismatch or prompt is null. Prompt: $prompt, CorrectAnswer: $fullCorrectAnswer")
                isCorrect = userInput.equals(fullCorrectAnswer, ignoreCase = true)
            }
        } else {
            isCorrect = userInput.equals(currentQ.correctAnswer, ignoreCase = true)
        }

        if (isCorrect) {
            _score.value = (_score.value ?: 0) + 1
            Log.d(TAG, "Answer CORRECT. New score: ${_score.value}")
        } else {
            Log.d(TAG, "Answer INCORRECT.")
        }

        val currentIndex = _currentQuestionIndex.value ?: 0
        val totalQuestions = _currentQuizQuestions.value?.size ?: 0
        Log.d(TAG, "Current index: $currentIndex, Total questions: $totalQuestions")

        if (currentIndex + 1 < totalQuestions) {
            _currentQuestionIndex.value = currentIndex + 1
            Log.d(TAG, "Moving to next question, new index: ${_currentQuestionIndex.value}")
            updateCurrentQuestion()
        } else {
            Log.d(TAG, "Last question answered. Finishing quiz.")
            _isQuizFinished.postValue(true)
        }
    }

    fun loadAllQuizzes() {
        viewModelScope.launch {
            Log.d(TAG, "loadAllQuizzes called")
            _isLoading.postValue(true)
            try {
                quizDao.getAllQuizzesWithPlaylistName().collectLatest { quizList ->
                    _quizzes.postValue(quizList)
                    Log.d(TAG, "loadAllQuizzes: quizzes updated with ${quizList.size} items.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading quizzes: ${e.message}", e)
                _message.postValue("Error loading quizzes: ${e.message}")
            } finally {
                _isLoading.postValue(false)
                Log.d(TAG, "loadAllQuizzes finished, isLoading set to false.")
            }
        }
    }

    fun loadAvailablePlaylists() {
        viewModelScope.launch {
            try {
                playlistDao.getAllPlaylists().collectLatest { playlists ->
                    _availablePlaylists.postValue(playlists)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading available playlists: ${e.message}", e)
                _message.postValue("Error loading playlists: ${e.message}")
            }
        }
    }

    fun deleteQuiz(quizInfo: QuizWithPlaylistInfo) {
        viewModelScope.launch {
            Log.d(TAG, "deleteQuiz called for: ${quizInfo.quiz.name}")
            _isLoading.postValue(true)
            try {
                withContext(Dispatchers.IO) { quizDao.deleteQuiz(quizInfo.quiz) }
                _message.postValue("Quiz '${quizInfo.quiz.name}' deleted.")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting quiz: ${e.message}", e)
                _message.postValue("Error deleting quiz: ${e.message}")
            } finally {
                _isLoading.postValue(false)
                Log.d(TAG, "deleteQuiz finished, isLoading set to false.")
            }
        }
    }

    fun resetQuizState() {
        Log.d(TAG, "resetQuizState called")
        _currentQuizDetails.postValue(null)
        _currentQuizQuestions.postValue(emptyList())
        _currentQuestion.postValue(null)
        _currentQuestionIndex.postValue(0)
        _score.postValue(0)
        _isQuizFinished.postValue(false)
    }

    fun clearMessage() {
        _message.value = null
    }
}
