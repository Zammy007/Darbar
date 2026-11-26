package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.DailyQuiz
import com.example.model.QuizAttempt
import com.example.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val QUIZ_TAG = "DailyQuizRepo"

class DailyQuizRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "A clear, factually accurate general knowledge question."),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct multiple-choice answer options."
                        ),
                        "correctIndex" to Schema.integer(description = "Zero-based index (0 to 3) of the single correct option."),
                        "explanation" to Schema.string(description = "A concise 1-2 sentence factual explanation of the correct answer."),
                        "topic" to Schema.string(description = "Topic tag: Bangladesh, India, World, Science, History, Geography, or Current Awareness.")
                    )
                ),
                description = "Exactly 10 general knowledge multiple-choice questions."
            )
        )
    )

    // Lazily initialized Firebase AI Logic model so FirebaseApp/AppCheck runs first
    private val geminiModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
                temperature = 0.6f
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            requestOptions = RequestOptions(timeoutInMillis = 55_000L)
        )
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing daily quizzes.")
    }

    fun todayDateKey(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    /**
     * Loads today's shared Daily Quiz from Firestore (`daily_quizzes/{dateKey}`).
     * If not yet generated today, calls Gemini via Firebase AI Logic to generate 10 GK questions
     * and persists them to Firestore so every student sees the exact same daily quiz.
     */
    suspend fun getOrGenerateTodayQuiz(): Result<DailyQuiz> {
        val dateKey = todayDateKey()
        val docPath = "daily_quizzes/$dateKey"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("daily_quizzes").document(dateKey)

            // 1. Check if today's quiz already exists in Firestore
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val parsed = parseQuizSnapshot(existingSnap, dateKey)
                if (parsed != null && parsed.questions.size == 10) {
                    return Result.success(parsed)
                }
            }

            // 2. Generate 10 factually accurate GK questions using Gemini (Firebase AI Logic)
            val prompt = buildString {
                appendLine("Generate a daily General Knowledge quiz for high school and college students for date $dateKey.")
                appendLine("Requirements:")
                appendLine("- Exactly 10 multiple-choice questions.")
                appendLine("- Balanced mix of topics: Bangladesh (2), India (2), World (1), Science (2), History (1), Geography (1), Current Awareness (1).")
                appendLine("- Each question must have exactly 4 plausible options, exactly 1 objectively correct answer (correctIndex 0-3), a concise factual explanation (1-2 sentences), and a topic label.")
                appendLine("- Double-check every single fact for 100% accuracy. Avoid ambiguous or trick questions.")
            }

            val response = withTimeout(60_000L) {
                geminiModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("Gemini returned an empty response while generating today's quiz.")

            val generatedQuestions = parseGeminiQuizJson(rawJson)
            if (generatedQuestions.size != 10) {
                throw IllegalStateException("Expected 10 questions from AI, received ${generatedQuestions.size}.")
            }

            // 3. Persist today's shared quiz to Firestore
            val payload = mapOf(
                "dateKey" to dateKey,
                "createdBy" to uid,
                "visibility" to "public",
                "source" to "gemini",
                "questions" to generatedQuestions.map { q ->
                    mapOf(
                        "question" to q.question,
                        "options" to q.options,
                        "correctIndex" to q.correctIndex.toLong(),
                        "explanation" to q.explanation,
                        "topic" to q.topic
                    )
                },
                "createdAt" to FieldValue.serverTimestamp()
            )

            try {
                docRef.set(payload).await()
            } catch (writeErr: FirebaseFirestoreException) {
                // If another student generated today's quiz concurrently, read the canonical shared doc
                if (writeErr.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    writeErr.code == FirebaseFirestoreException.Code.ALREADY_EXISTS
                ) {
                    val concurrentSnap = docRef.get().await()
                    val canonical = parseQuizSnapshot(concurrentSnap, dateKey)
                    if (canonical != null && canonical.questions.size == 10) {
                        return Result.success(canonical)
                    }
                }
                throw writeErr
            }

            Result.success(
                DailyQuiz(
                    dateKey = dateKey,
                    questions = generatedQuestions,
                    source = "gemini",
                    createdAt = Timestamp.now()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(QUIZ_TAG, "Gemini quiz generation timed out", e)
            Result.failure(IllegalStateException("Quiz generation timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(QUIZ_TAG, "Failed to load or generate today's quiz", e)
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.WRITE, docPath)
            }
            Result.failure(e)
        }
    }

    suspend fun getTodayAttempt(): Result<QuizAttempt?> {
        val dateKey = todayDateKey()
        var docPath = "users/unauthenticated/quiz_attempts/$dateKey"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/quiz_attempts/$dateKey"
            val snap = db.collection("users")
                .document(uid)
                .collection("quiz_attempts")
                .document(dateKey)
                .get()
                .await()

            if (!snap.exists()) {
                return Result.success(null)
            }
            val attempt = snap.toObject(
                QuizAttempt::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveTodayAttempt(
        score: Int,
        totalQuestions: Int,
        selectedAnswers: List<Int>
    ): Result<Unit> {
        val dateKey = todayDateKey()
        var docPath = "users/unauthenticated/quiz_attempts/$dateKey"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid/quiz_attempts/$dateKey"
            val docRef = db.collection("users")
                .document(uid)
                .collection("quiz_attempts")
                .document(dateKey)

            val boundedScore = score.coerceIn(0, 10).toLong()
            val boundedTotal = totalQuestions.coerceIn(1, 10).toLong()
            val answerLongs = selectedAnswers.take(10).map { it.toLong() }

            val existing = docRef.get().await()
            if (existing.exists()) {
                // Partial update preserving immutable completedAt, userId, dateKey
                docRef.update(
                    mapOf(
                        "score" to boundedScore,
                        "totalQuestions" to boundedTotal,
                        "selectedAnswers" to answerLongs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "score" to boundedScore,
                        "totalQuestions" to boundedTotal,
                        "selectedAnswers" to answerLongs,
                        "completedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun parseGeminiQuizJson(rawJson: String): List<QuizQuestion> {
        val root = JSONObject(rawJson)
        val questionsArray = root.getJSONArray("questions")
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until questionsArray.length()) {
            val item = questionsArray.getJSONObject(i)
            val questionText = item.getString("question").trim()
            val optsArray = item.getJSONArray("options")
            val options = (0 until optsArray.length()).map { optsArray.getString(it).trim() }
            val correctIndex = item.getInt("correctIndex").coerceIn(0, (options.size - 1).coerceAtLeast(0))
            val explanation = item.optString("explanation", "").trim()
            val topic = item.optString("topic", "General Knowledge").trim().ifEmpty { "General Knowledge" }
            if (questionText.isNotEmpty() && options.size == 4) {
                list.add(
                    QuizQuestion(
                        question = questionText,
                        options = options,
                        correctIndex = correctIndex,
                        explanation = explanation,
                        topic = topic
                    )
                )
            }
        }
        return list.take(10)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseQuizSnapshot(snapshot: DocumentSnapshot, fallbackDateKey: String): DailyQuiz? {
        val data = snapshot.data ?: return null
        val rawQuestions = data["questions"] as? List<*> ?: return null
        val questions = rawQuestions.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val qText = map["question"] as? String ?: return@mapNotNull null
            val opts = (map["options"] as? List<*>)?.mapNotNull { it as? String } ?: return@mapNotNull null
            if (opts.size != 4) return@mapNotNull null
            val correctIdx = ((map["correctIndex"] as? Number)?.toInt() ?: 0).coerceIn(0, 3)
            val explanation = (map["explanation"] as? String).orEmpty()
            val topic = (map["topic"] as? String).orEmpty().ifBlank { "General Knowledge" }
            QuizQuestion(
                question = qText,
                options = opts,
                correctIndex = correctIdx,
                explanation = explanation,
                topic = topic
            )
        }
        if (questions.isEmpty()) return null
        return DailyQuiz(
            dateKey = (data["dateKey"] as? String) ?: fallbackDateKey,
            questions = questions,
            source = (data["source"] as? String) ?: "gemini",
            createdAt = snapshot.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }
}
