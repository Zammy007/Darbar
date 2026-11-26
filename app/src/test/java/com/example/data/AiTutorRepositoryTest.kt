package com.example.data

import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.Subject
import com.google.firebase.ai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTutorRepositoryTest {

    private val repo = AiTutorRepository()

    @Test
    fun buildPriorHistory_excludesWelcomeErrorsStreamingAndTrailingUser() {
        val messages = listOf(
            ChatMessage(id = "welcome_1", role = ChatRole.MODEL, text = "Welcome!"),
            ChatMessage(id = "u1", role = ChatRole.USER, text = "What is Ohm's law?"),
            ChatMessage(id = "m1", role = ChatRole.MODEL, text = "V = IR"),
            ChatMessage(id = "u2", role = ChatRole.USER, text = "Failed question"),
            ChatMessage(id = "e1", role = ChatRole.MODEL, text = "Error occurred", isError = true),
            ChatMessage(id = "s1", role = ChatRole.MODEL, text = "partial", isStreaming = true)
        )

        val history = repo.buildPriorHistory(messages)
        // Must end with a model turn so the next sendMessage(userPrompt) alternates cleanly
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("What is Ohm's law?", (history[0].parts.first() as TextPart).text)
        assertEquals("model", history[1].role)
        assertEquals("V = IR", (history[1].parts.first() as TextPart).text)
    }

    @Test
    fun buildPriorHistory_mergesConsecutiveSameRoleAndDropsLeadingModel() {
        val messages = listOf(
            ChatMessage(id = "m0", role = ChatRole.MODEL, text = "Orphan model msg"),
            ChatMessage(id = "u1", role = ChatRole.USER, text = "Part 1"),
            ChatMessage(id = "u2", role = ChatRole.USER, text = "Part 2"),
            ChatMessage(id = "m1", role = ChatRole.MODEL, text = "Answer 1")
        )

        val history = repo.buildPriorHistory(messages)
        assertEquals(2, history.size)
        assertEquals("user", history[0].role)
        assertEquals("Part 1\n\nPart 2", (history[0].parts.first() as TextPart).text)
        assertEquals("model", history[1].role)
        assertEquals("Answer 1", (history[1].parts.first() as TextPart).text)
    }

    @Test
    fun buildPriorHistory_returnsEmptyWhenOnlyWelcomeExists() {
        val messages = listOf(
            ChatMessage(id = "welcome_init", role = ChatRole.MODEL, text = "Hello student!")
        )
        assertTrue(repo.buildPriorHistory(messages).isEmpty())
    }

    @Test
    fun buildContextualPrompt_includesStudentClassAndSubjectFocus() {
        val prompt = repo.buildContextualPrompt(
            userMessage = "সমাস কাকে বলে?",
            focusSubject = Subject.BENGALI,
            studentClass = "Class 10"
        )
        assertEquals("[Student Level: Class 10 | Subject Focus: Bengali (বাংলা)]\nসমাস কাকে বলে?", prompt)
    }
}
