package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.LanguageDetector
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.LocalAssistantRepository
import com.example.domain.model.ApprovedKnowledge
import com.example.domain.model.AssistantConfig
import com.example.domain.model.CallHandlingMode
import com.example.domain.model.CallRecord
import com.example.domain.model.ContactHandlingRule
import com.example.domain.model.PermissionGateType
import com.example.domain.model.SpeakerRole
import com.example.domain.model.SupportedLanguage
import com.example.domain.model.ConversationTurn
import com.example.ui.theme.AICallAssistantTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app_name matches AI Call Assistant`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Call Assistant", appName)
    }

    @Test
    fun `language detector identifies scripts and mixed romanized speech`() {
        assertEquals(SupportedLanguage.HINDI, LanguageDetector.detectLanguage("नमस्ते मुझे बात करनी है"))
        assertEquals(SupportedLanguage.HINDI, LanguageDetector.detectLanguage("Mujhe Raman ji se office ke kaam se baat karni thi"))
        assertEquals(SupportedLanguage.PUNJABI, LanguageDetector.detectLanguage("ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ ਜੀ"))
        assertEquals(SupportedLanguage.PUNJABI, LanguageDetector.detectLanguage("Sat Sri Akal mainu kal gal karni si"))
        assertEquals(SupportedLanguage.TAMIL, LanguageDetector.detectLanguage("வணக்கம்"))
        assertEquals(SupportedLanguage.TELUGU, LanguageDetector.detectLanguage("నమస్కారం"))
        assertEquals(SupportedLanguage.BENGALI, LanguageDetector.detectLanguage("নমস্কার"))
        assertEquals(SupportedLanguage.GUJARATI, LanguageDetector.detectLanguage("નમસ્તે"))
        assertEquals(SupportedLanguage.KANNADA, LanguageDetector.detectLanguage("ನಮಸ್ಕಾರ"))
        assertEquals(SupportedLanguage.MALAYALAM, LanguageDetector.detectLanguage("നമസ്കാരം"))
        assertEquals(SupportedLanguage.URDU, LanguageDetector.detectLanguage("السلام علیکم"))
        assertEquals(SupportedLanguage.ENGLISH, LanguageDetector.detectLanguage("Hello, please schedule a callback for tomorrow"))
    }

    @Test
    fun `placeholder ai repository enforces identity disclosure, approved answers, and permission gates`() = runBlocking {
        val repo = PlaceholderAiRepository()
        val config = AssistantConfig(userName = "Raman ji")
        val knowledge = listOf(
            ApprovedKnowledge(
                title = "Office Hours",
                category = "Office",
                approvedAnswer = "Office is open 9 AM to 7 PM."
            )
        )

        val greeting = repo.generateUserGreetingPrompt("Amit", "+91 9876543210", config)
        assertTrue(greeting.contains("Raman ji"))
        assertTrue(greeting.contains("Amit"))

        val opening = repo.generateCallerOpening("Amit", config, SupportedLanguage.HINDI)
        assertTrue(opening.contains("AI assistant", ignoreCase = true))

        // Permission gate for personal info
        val infoReply = repo.generateCallerReply(
            callerUtterance = "Please share Raman ji home address",
            callerDisplayName = "Amit",
            history = emptyList(),
            config = config,
            approvedKnowledge = knowledge
        )
        assertNotNull(infoReply.permissionRequest)
        assertEquals(PermissionGateType.PERSONAL_INFO, infoReply.permissionRequest?.gateType)

        // Approved knowledge match
        val faqReply = repo.generateCallerReply(
            callerUtterance = "What are office hours?",
            callerDisplayName = "Amit",
            history = emptyList(),
            config = config,
            approvedKnowledge = knowledge
        )
        assertTrue(faqReply.replyText.contains("9 AM to 7 PM"))

        // Summary generation
        val summary = repo.summarizeCall(
            callerName = "Amit",
            phoneNumber = "+91 9876543210",
            durationSeconds = 42,
            turns = listOf(
                ConversationTurn(
                    id = "1",
                    speaker = SpeakerRole.CALLER,
                    text = "Mujhe Raman ji se office ke kaam ke liye baat karni thi",
                    language = SupportedLanguage.HINDI
                )
            ),
            config = config
        )
        assertEquals(SupportedLanguage.HINDI, summary.detectedLanguage)
        assertTrue(summary.postCallUserBriefing.contains("Amit"))
    }

    @Test
    fun `room repository persists and deletes user records cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = LocalAssistantRepository(db)

        repo.saveTrustedContact("Amit", "+91 9876543210", "Colleague", ContactHandlingRule.GREET_FIRST)
        assertNotNull(repo.findTrustedContact("+91 9876543210"))

        repo.blockNumber("+91 9999900000", "Telemarketer")
        assertTrue(repo.isBlocked("+91 9999900000"))

        val id = repo.saveCallRecord(
            CallRecord(
                callerName = "Amit",
                phoneNumber = "+91 9876543210",
                durationSeconds = 30,
                detectedLanguage = SupportedLanguage.HINDI,
                handlingMode = CallHandlingMode.AI_HANDLED,
                reasonForCalling = "Office project update",
                aiSummary = "Summary text",
                requestedAction = "Call back",
                callerMessage = "Please call back at 5 PM",
                transcript = "Caller: Hello",
                callbackReminderAt = System.currentTimeMillis() + 60000L,
                isSavedMessage = true
            )
        )
        assertTrue(id > 0)
        assertEquals(1, repo.observeCallRecords().first().size)

        repo.clearAllUserData()
        assertEquals(0, repo.observeCallRecords().first().size)
    }

    @Test
    fun `compose ui navigates 5 tabs and runs incoming call greet-first to let-ai-talk flow`() {
        composeTestRule.setContent {
            AICallAssistantTheme {
                AICallAssistantApp()
            }
        }
        composeTestRule.waitForIdle()

        // Verify all 5 bottom navigation tabs exist
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_calls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_ai_assistant").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_history").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_settings").assertIsDisplayed()

        // Trigger incoming call greeter from Home
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("btn_trigger_incoming_call").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("btn_trigger_incoming_call").performScrollTo().performClick()

        // Verify Phase 1 Greet User First options
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("btn_let_ai_talk").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("btn_answer_myself").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_let_ai_talk").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_reject_call").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_silent_mode").performScrollTo().assertIsDisplayed()

        // Authorize Let AI Talk -> verify 4 User Controls appear
        composeTestRule.onNodeWithTag("btn_let_ai_talk").performClick()

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("btn_take_over_call").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("btn_take_over_call").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_stop_ai").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_mute_ai").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_end_call").performScrollTo().assertIsDisplayed()

        // Navigate to AI Assistant tab
        composeTestRule.onNodeWithTag("nav_ai_assistant").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Personal AI Brain").assertIsDisplayed()
    }
}
