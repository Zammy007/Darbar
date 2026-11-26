package com.example.data

import android.util.Log
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.Subject
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TUTOR_TAG = "AiTutorRepo"
private const val MAX_HISTORY_TURNS = 16
private const val MAX_OUTPUT_TOKENS = 4096
private const val STREAM_FIRST_TOKEN_TIMEOUT_MS = 20_000L
private const val STREAM_TOTAL_TIMEOUT_MS = 60_000L
private const val FALLBACK_UNARY_TIMEOUT_MS = 35_000L

private class FirstTokenTimeoutException : Exception("No response chunk received within first-token window.")

/**
 * Multi-turn Gemini Study Assistant powered by Firebase AI Logic (`Firebase.ai`).
 * Uses `gemini-3.8-flash` (`ThinkingLevel.LOW`) with fresh `startChat(history)` sessions built
 * from strictly alternating committed turns so errors/retries never corrupt chat state, plus
 * a 20s first-token watchdog and automatic unary fallback to `gemini-3.5-flash-lite` (`ThinkingLevel.MINIMAL`).
 */
class AiTutorRepository {

    private val systemPromptText = """
        You are "দরবার AI Tutor" (Darbar AI Tutor), a warm, patient, and knowledgeable study mentor for high school and college students on the দরবার (DARBAR) study platform.
        Your role:
        - Help students master all 6 Darbar subjects: Bengali (বাংলা), English, Physics (পদার্থবিজ্ঞান), Mathematics (গণিত), Chemistry (রসায়ন), and Biology (জীববিজ্ঞান).
        - Match the student's language naturally: reply in clear Bengali (বাংলা) if they write in Bengali, or clear English if they write in English (or bilingual when helpful for Bengali subject terms).
        - Break complex concepts into concise, easy-to-follow steps, key formulas, and memorable examples.
        - Keep answers clear, well-structured, and easy to read on a mobile screen.
    """.trimIndent()

    // Primary streaming model: gemini-3.8-flash with LOW thinking and generous token budget for Bengali + English
    private val primaryModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = MAX_OUTPUT_TOKENS
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            systemInstruction = content {
                text(systemPromptText)
            },
            requestOptions = RequestOptions(timeoutInMillis = 45_000L)
        )
    }

    // Ultra-fast unary fallback model: gemini-3.5-flash-lite with MINIMAL thinking
    private val fastFallbackModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = MAX_OUTPUT_TOKENS
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MINIMAL
                }
            },
            systemInstruction = content {
                text(systemPromptText)
            },
            requestOptions = RequestOptions(timeoutInMillis = 35_000L)
        )
    }

    /**
     * Generates a multi-turn Gemini reply from clean committed conversation history + the new user prompt.
     * Streams chunks via [onChunk] and returns the complete final text.
     * If streaming stalls before emitting any text or encounters a stream error, automatically falls back
     * to a fast unary `sendMessage` request so the student receives an answer without hanging.
     */
    suspend fun generateReplyStream(
        committedHistory: List<ChatMessage>,
        userMessage: String,
        focusSubject: Subject? = null,
        studentClass: String? = null,
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val trimmed = userMessage.trim()
        require(trimmed.isNotEmpty()) { "Message cannot be empty." }

        val promptToSend = buildContextualPrompt(
            userMessage = trimmed,
            focusSubject = focusSubject,
            studentClass = studentClass
        )
        val priorHistory = buildPriorHistory(committedHistory)

        val accumulated = StringBuilder()
        var receivedAnyChunk = false
        var streamError: Exception? = null
        var lastResponse: GenerateContentResponse? = null

        // Attempt 1: Live multi-turn chat stream with a 20s first-token watchdog
        try {
            val chat = primaryModel.startChat(history = priorHistory)
            withTimeout(STREAM_TOTAL_TIMEOUT_MS) {
                coroutineScope {
                    val watchdogJob = launch {
                        delay(STREAM_FIRST_TOKEN_TIMEOUT_MS)
                        if (!receivedAnyChunk) {
                            throw FirstTokenTimeoutException()
                        }
                    }
                    try {
                        chat.sendMessageStream(promptToSend).collect { chunk ->
                            lastResponse = chunk
                            val delta = chunk.text.orEmpty()
                            if (delta.isNotEmpty()) {
                                if (!receivedAnyChunk) {
                                    receivedAnyChunk = true
                                    watchdogJob.cancel()
                                }
                                accumulated.append(delta)
                                onChunk(delta)
                            }
                        }
                    } finally {
                        watchdogJob.cancel()
                    }
                }
            }
        } catch (e: FirstTokenTimeoutException) {
            Log.w(TUTOR_TAG, "Primary stream produced no text within ${STREAM_FIRST_TOKEN_TIMEOUT_MS}ms; switching to fast unary fallback")
            streamError = e
        } catch (e: TimeoutCancellationException) {
            val partial = accumulated.toString().trim()
            if (partial.isNotEmpty()) {
                return@withContext partial
            }
            Log.w(TUTOR_TAG, "Primary stream timed out before producing text; switching to fast unary fallback")
            streamError = e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TUTOR_TAG, "Primary streaming failed; trying fast unary fallback", e)
            streamError = e
        }

        val streamedText = accumulated.toString().trim()
        if (streamedText.isNotEmpty()) {
            return@withContext streamedText
        }

        // Attempt 2: Fast unary fallback (avoids SSE stream stalls and uses ultra-fast flash-lite)
        try {
            val fallbackChat = fastFallbackModel.startChat(history = priorHistory)
            val unaryResponse = withTimeout(FALLBACK_UNARY_TIMEOUT_MS) {
                fallbackChat.sendMessage(promptToSend)
            }
            lastResponse = unaryResponse
            val unaryText = unaryResponse.text.orEmpty().trim()
            if (unaryText.isNotEmpty()) {
                onChunk(unaryText)
                return@withContext unaryText
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TUTOR_TAG, "Fast unary fallback also timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (fallbackErr: Exception) {
            Log.e(TUTOR_TAG, "Fast unary fallback failed", fallbackErr)
            throw buildReadableError(fallbackErr)
        }

        val blockReason = lastResponse?.promptFeedback?.blockReason
        if (blockReason != null) {
            throw IllegalStateException("This prompt couldn't be answered ($blockReason). Please rephrase your study question.")
        }

        if (streamError != null) {
            Log.e(TUTOR_TAG, "Gemini chat generation failed", streamError)
            throw buildReadableError(streamError)
        }

        throw IllegalStateException("Gemini returned an empty response. Please tap Retry.")
    }

    /**
     * Converts committed UI messages into strictly alternating `(user, model)` Content turns
     * suitable for `GenerativeModel.startChat(history = ...)`:
     * - Drops welcome banners, error bubbles, in-progress streaming bubbles, and blank entries.
     * - Always starts with a `user` turn and ends with a `model` turn (so the next `sendMessage` user turn alternates cleanly).
     */
    internal fun buildPriorHistory(committedHistory: List<ChatMessage>): List<Content> {
        val cleaned = committedHistory
            .filter { msg ->
                !msg.id.startsWith("welcome_") &&
                    !msg.isError &&
                    !msg.isStreaming &&
                    msg.text.isNotBlank()
            }
            .takeLast(MAX_HISTORY_TURNS)

        val mergedTurns = mutableListOf<Pair<String, String>>()
        for (msg in cleaned) {
            val role = if (msg.role == ChatRole.USER) "user" else "model"
            val text = msg.text.trim()
            if (mergedTurns.isEmpty() && role != "user") continue
            val last = mergedTurns.lastOrNull()
            if (last != null && last.first == role) {
                mergedTurns[mergedTurns.lastIndex] = role to "${last.second}\n\n$text"
            } else {
                mergedTurns.add(role to text)
            }
        }

        // Prior history passed to startChat() must end with a model turn before sendMessage() adds a user turn
        while (mergedTurns.isNotEmpty() && mergedTurns.last().first == "user") {
            mergedTurns.removeAt(mergedTurns.lastIndex)
        }

        return mergedTurns.map { (role, text) ->
            content(role = role) { text(text) }
        }
    }

    internal fun buildContextualPrompt(
        userMessage: String,
        focusSubject: Subject?,
        studentClass: String?
    ): String {
        val contextParts = buildList {
            if (!studentClass.isNullOrBlank()) {
                add("Student Level: ${studentClass.trim()}")
            }
            if (focusSubject != null) {
                add("Subject Focus: ${focusSubject.titleEn} (${focusSubject.titleBn})")
            }
        }
        return if (contextParts.isEmpty()) {
            userMessage
        } else {
            "[${contextParts.joinToString(" | ")}]\n$userMessage"
        }
    }

    private fun buildReadableError(error: Exception): Exception {
        val raw = generateSequence<Throwable>(error) { it.cause }
            .mapNotNull { it.localizedMessage ?: it.message }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        val lower = raw.lowercase()
        val friendly = when {
            lower.contains("403") || lower.contains("app check") || lower.contains("attestation") ->
                "App Check verification failed (403). Please reopen the app and try again. ($raw)"
            lower.contains("unable to resolve host") || lower.contains("network") || lower.contains("failed to connect") ->
                "No internet connection. Please check your network and tap Retry."
            lower.contains("quota") || lower.contains("429") || lower.contains("resource_exhausted") ->
                "Gemini is temporarily busy (rate limit reached). Please wait a moment and tap Retry."
            raw.isNotBlank() -> raw
            else -> "Couldn't reach Gemini right now. Please check your connection and tap Retry."
        }
        return IllegalStateException(friendly, error)
    }
}
