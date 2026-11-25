package com.example.model

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.SubjectBengaliBg
import com.example.ui.theme.SubjectBengaliColor
import com.example.ui.theme.SubjectBiologyBg
import com.example.ui.theme.SubjectBiologyColor
import com.example.ui.theme.SubjectChemistryBg
import com.example.ui.theme.SubjectChemistryColor
import com.example.ui.theme.SubjectEnglishBg
import com.example.ui.theme.SubjectEnglishColor
import com.example.ui.theme.SubjectMathBg
import com.example.ui.theme.SubjectMathColor
import com.example.ui.theme.SubjectPhysicsBg
import com.example.ui.theme.SubjectPhysicsColor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

// Exact upload password constant — case-sensitive, strict equality only
const val UPLOAD_PASSWORD = "Tana007"

const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024 // 8 MB max for Firestore chunked storage
const val FILE_CHUNK_RAW_BYTES = 450_000     // ~600 KB Base64 per chunk (< 1 MiB Firestore doc limit)

val ALLOWED_FILE_EXTENSIONS = setOf(
    "pdf", "jpg", "jpeg", "png", "webp",
    "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt"
)

val PREVIEWABLE_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

enum class Subject(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val tagline: String,
    val accentColor: Color,
    val lightTint: Color,
    val icon: ImageVector
) {
    BENGALI(
        id = "bengali",
        titleEn = "Bengali",
        titleBn = "বাংলা",
        tagline = "সাহিত্য ও ব্যাকরণ",
        accentColor = SubjectBengaliColor,
        lightTint = SubjectBengaliBg,
        icon = Icons.Rounded.AutoStories
    ),
    ENGLISH(
        id = "english",
        titleEn = "English",
        titleBn = "ইংরেজি",
        tagline = "Grammar & Literature",
        accentColor = SubjectEnglishColor,
        lightTint = SubjectEnglishBg,
        icon = Icons.Rounded.Translate
    ),
    PHYSICS(
        id = "physics",
        titleEn = "Physics",
        titleBn = "পদার্থবিজ্ঞান",
        tagline = "Mechanics, Optics & Waves",
        accentColor = SubjectPhysicsColor,
        lightTint = SubjectPhysicsBg,
        icon = Icons.Rounded.Bolt
    ),
    MATHEMATICS(
        id = "mathematics",
        titleEn = "Mathematics",
        titleBn = "গণিত",
        tagline = "Algebra, Calculus & Geometry",
        accentColor = SubjectMathColor,
        lightTint = SubjectMathBg,
        icon = Icons.Rounded.Calculate
    ),
    CHEMISTRY(
        id = "chemistry",
        titleEn = "Chemistry",
        titleBn = "রসায়ন",
        tagline = "Organic, Inorganic & Lab",
        accentColor = SubjectChemistryColor,
        lightTint = SubjectChemistryBg,
        icon = Icons.Rounded.Biotech
    ),
    BIOLOGY(
        id = "biology",
        titleEn = "Biology",
        titleBn = "জীববিজ্ঞান",
        tagline = "Botany, Zoology & Genetics",
        accentColor = SubjectBiologyColor,
        lightTint = SubjectBiologyBg,
        icon = Icons.Rounded.Eco
    );

    companion object {
        fun fromId(raw: String): Subject =
            entries.firstOrNull { it.id == raw } ?: BENGALI
    }
}

enum class SortOrder(val label: String) {
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
    A_TO_Z("A to Z")
}

enum class SubjectTab {
    VIEW,
    UPLOAD
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class StudyDocument(
    @DocumentId val id: String = "",
    val subjectId: String = "",
    val title: String = "",
    val description: String? = null,
    val fileName: String = "",
    val fileExtension: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L,
    val chunkCount: Long = 1L,
    val uploadComplete: Boolean = false,
    val visibility: String = "public",
    val uploaderId: String = "",
    val uploaderName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun canPreview(): Boolean {
        val ext = fileExtension.lowercase()
        return ext == "pdf" || ext in PREVIEWABLE_IMAGE_EXTENSIONS
    }

    fun isPdf(): Boolean = fileExtension.lowercase() == "pdf"
    fun isImage(): Boolean = fileExtension.lowercase() in PREVIEWABLE_IMAGE_EXTENSIONS
}

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val topic: String = "General Knowledge"
)

data class DailyQuiz(
    val dateKey: String,
    val questions: List<QuizQuestion>,
    val source: String = "gemini",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 10L,
    val selectedAnswers: List<Long> = emptyList(),
    @ServerTimestamp val completedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PickedFileInfo(
    val uriString: String,
    val fileName: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long
)

data class PreviewPayload(
    val document: StudyDocument,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PreviewPayload) return false
        return document.id == other.document.id && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * document.id.hashCode() + bytes.contentHashCode()
}

// Curated, factually verified fallback bank offered only if Gemini generation fails
val FALLBACK_GK_QUESTIONS: List<QuizQuestion> = listOf(
    QuizQuestion(
        question = "Which river is the longest river flowing through Bangladesh?",
        options = listOf("Padma", "Meghna", "Jamuna", "Surma-Meghna River System"),
        correctIndex = 3,
        explanation = "The Surma-Meghna River System is the longest river system within Bangladesh, spanning roughly 669 km.",
        topic = "Bangladesh"
    ),
    QuizQuestion(
        question = "Who was the first Nobel Laureate from Asia, awarded the Nobel Prize in Literature in 1913?",
        options = listOf("C. V. Raman", "Rabindranath Tagore", "Amartya Sen", "Kazi Nazrul Islam"),
        correctIndex = 1,
        explanation = "Rabindranath Tagore won the 1913 Nobel Prize in Literature primarily for his poetry collection Gitanjali.",
        topic = "History"
    ),
    QuizQuestion(
        question = "What is the SI unit of electrical resistance?",
        options = listOf("Volt", "Ampere", "Ohm", "Watt"),
        correctIndex = 2,
        explanation = "Electrical resistance is measured in Ohms (Ω), named after German physicist Georg Simon Ohm.",
        topic = "Science"
    ),
    QuizQuestion(
        question = "Which Indian state has the longest coastline?",
        options = listOf("Maharashtra", "Tamil Nadu", "Andhra Pradesh", "Gujarat"),
        correctIndex = 3,
        explanation = "Gujarat has the longest coastline in India, stretching over 1,600 kilometers along the Arabian Sea.",
        topic = "India"
    ),
    QuizQuestion(
        question = "Which planet in our Solar System has the most confirmed moons?",
        options = listOf("Jupiter", "Saturn", "Uranus", "Neptune"),
        correctIndex = 1,
        explanation = "Saturn leads the Solar System with well over 140 confirmed natural satellites, surpassing Jupiter.",
        topic = "Science"
    ),
    QuizQuestion(
        question = "In which year was International Mother Language Day (21 February) proclaimed by UNESCO?",
        options = listOf("1952", "1971", "1999", "2005"),
        correctIndex = 2,
        explanation = "UNESCO proclaimed 21 February as International Mother Language Day in November 1999 to honor the 1952 Bengali Language Movement.",
        topic = "Bangladesh"
    ),
    QuizQuestion(
        question = "Which mountain pass connects the Kashmir Valley with Ladakh in India?",
        options = listOf("Nathu La", "Zoji La", "Rohtang Pass", "Shipki La"),
        correctIndex = 1,
        explanation = "Zoji La is a high mountain pass in the Himalayas that links Srinagar in the Kashmir Valley with Leh in Ladakh.",
        topic = "Geography"
    ),
    QuizQuestion(
        question = "Which gas makes up approximately 78% of Earth's atmosphere?",
        options = listOf("Oxygen", "Carbon Dioxide", "Argon", "Nitrogen"),
        correctIndex = 3,
        explanation = "Dry air in Earth's atmosphere is composed of about 78% nitrogen, 21% oxygen, and 1% other gases.",
        topic = "Science"
    ),
    QuizQuestion(
        question = "Where is the headquarters of the International Court of Justice (ICJ) located?",
        options = listOf("Geneva, Switzerland", "New York, USA", "The Hague, Netherlands", "Vienna, Austria"),
        correctIndex = 2,
        explanation = "The International Court of Justice is seated at the Peace Palace in The Hague, Netherlands.",
        topic = "World"
    ),
    QuizQuestion(
        question = "Which ancient university in Bihar, India, was a renowned center of Buddhist learning from the 5th to 12th century CE?",
        options = listOf("Takshashila", "Nalanda", "Vikramashila", "Somapura"),
        correctIndex = 1,
        explanation = "Nalanda Mahavihara was one of the world's earliest residential universities and is now a UNESCO World Heritage Site.",
        topic = "History"
    )
)
