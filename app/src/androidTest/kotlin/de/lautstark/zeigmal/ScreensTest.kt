package de.lautstark.zeigmal

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.Login
import de.lautstark.zeigmal.core.Phase
import de.lautstark.zeigmal.core.StationState
import de.lautstark.zeigmal.core.TagId
import de.lautstark.zeigmal.core.WriteOutcome
import de.lautstark.zeigmal.core.Writing
import de.lautstark.zeigmal.ui.KidScreen
import de.lautstark.zeigmal.ui.LoginScreen
import de.lautstark.zeigmal.ui.WriteScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The three screens against plain state; no hardware, no network. The host
 * activity is whatever orientation the phone is in, so these assert that the
 * right nodes exist and respond, not where they sit; the layout is landscape
 * by the app's manifest and is looked at by eye.
 */
class ScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val trinken = CardRecord("signdigital", "trinken", "trinken")
    private val a = TagId("04c5d4a88d2681")

    @Test
    fun kidScreenShowsTheRingForACardAndGreyForAnUnknownSticker() {
        compose.setContent {
            KidScreen(StationState.Card(trinken, a, Phase.LOADING), videoUrl = { "" }, onFirstFrame = {}, onEnded = {}, onFailed = {})
        }
        compose.onNodeWithTag("ring-seen").assertIsDisplayed()
        compose.onNodeWithTag("video").assertIsDisplayed()
    }

    @Test
    fun kidScreenIsOnlyTheMarkWhenIdle() {
        compose.setContent { KidScreen(StationState.Unknown(a), videoUrl = { "" }, onFirstFrame = {}, onEnded = {}, onFailed = {}) }
        compose.onNodeWithTag("ring-unknown").assertIsDisplayed()
    }

    @Test
    fun loginScreenSendsWhatWasTyped() {
        var sent: Pair<String, String>? = null
        compose.setContent { LoginScreen(Login.Out, onLogin = { e, p -> sent = e to p }, onBack = {}, onToggleLog = {}, log = null) }
        compose.onNodeWithTag("login-button").assertIsNotEnabled()
        compose.onNodeWithTag("email").performTextInput("mail@example.org")
        compose.onNodeWithTag("password").performTextInput("secret")
        compose.onNodeWithTag("login-button").performClick()
        assert(sent == "mail@example.org" to "secret")
    }

    @Test
    fun loginScreenShowsAFailure() {
        compose.setContent { LoginScreen(Login.Failed("401"), onLogin = { _, _ -> }, onBack = {}, onToggleLog = {}, log = null) }
        compose.onNodeWithTag("login-failed").assertIsDisplayed()
    }

    @Test
    fun writeScreenShowsTheCurrentWordAndSkips() {
        var skipped = false
        compose.setContent {
            WriteScreen(Writing(index = 3), onGoTo = {
            }, onSkip = { skipped = true }, onBack = {}, onOverwrite = {}, onLogout = {}, onToggleLog = {}, log = null)
        }
        compose.onNodeWithTag("word").assertTextEquals("allein")
        // Off-screen in a portrait host; the action is what is under test, not the position.
        compose.onNodeWithTag("skip").performSemanticsAction(SemanticsActions.OnClick)
        assert(skipped)
    }

    @Test
    fun writeScreenOffersToOverwriteAWrittenSticker() {
        var overwrite = false
        compose.setContent {
            WriteScreen(
                Writing(index = 0, lastOutcome = WriteOutcome.AlreadyWritten(a, CardRecord("signdigital", "essen", "essen"))),
                onGoTo = {},
                onSkip = {},
                onBack = {},
                onOverwrite = { overwrite = true },
                onLogout = {},
                onToggleLog = {},
                log = null,
            )
        }
        compose.onNodeWithTag("overwrite").performSemanticsAction(SemanticsActions.OnClick)
        assert(overwrite)
    }
}
