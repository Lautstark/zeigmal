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
import de.lautstark.zeigmal.core.AdultModel
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.CardSet
import de.lautstark.zeigmal.core.ListCard
import de.lautstark.zeigmal.core.ListEntry
import de.lautstark.zeigmal.core.ListSign
import de.lautstark.zeigmal.core.Lists
import de.lautstark.zeigmal.core.Login
import de.lautstark.zeigmal.core.Phase
import de.lautstark.zeigmal.core.Round
import de.lautstark.zeigmal.core.Search
import de.lautstark.zeigmal.core.StationState
import de.lautstark.zeigmal.core.TagId
import de.lautstark.zeigmal.core.WriteOutcome
import de.lautstark.zeigmal.core.WriteStatus
import de.lautstark.zeigmal.core.Writing
import de.lautstark.zeigmal.ui.KidScreen
import de.lautstark.zeigmal.ui.ListsScreen
import de.lautstark.zeigmal.ui.LoginScreen
import de.lautstark.zeigmal.ui.PinScreen
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
    private val ignoreFailure: (Round, String) -> Unit = { _, _ -> }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val trinken = CardRecord("signdigital", "trinken", "trinken")
    private val a = TagId("04c5d4a88d2681")

    private val box =
        CardSet(
            "sd-B",
            "Box",
            listOf("Abend(s)" to "abend-s", "aber" to "aber", "alle" to "alle", "allein" to "allein").map { (label, ref) ->
                ListCard(label, ref)
            },
        )

    @Test
    fun kidScreenShowsTheRingForACardAndGreyForAnUnknownSticker() {
        compose.setContent {
            KidScreen(
                StationState.Card(trinken, a, Phase.LOADING),
                videoUrl = { "" },
                onFirstFrame = {},
                onEnded = {},
                onFailed = ignoreFailure,
            )
        }
        compose.onNodeWithTag("ring-seen").assertIsDisplayed()
        compose.onNodeWithTag("video").assertIsDisplayed()
    }

    @Test
    fun kidScreenIsOnlyTheMarkWhenIdle() {
        compose.setContent {
            KidScreen(
                StationState.Unknown(a),
                videoUrl = { "" },
                onFirstFrame = {},
                onEnded = {},
                onFailed = ignoreFailure,
            )
        }
        compose.onNodeWithTag("ring-unknown").assertIsDisplayed()
    }

    @Test
    fun pinScreenHandsOverFourDigitsAndShowsARejection() {
        var entered: String? = null
        compose.setContent { PinScreen(isNew = false, rejected = false, onEntered = { entered = it }, onBack = {}) }
        listOf("4", "7", "1", "1").forEach { compose.onNodeWithTag("key-$it").performSemanticsAction(SemanticsActions.OnClick) }
        compose.waitUntil(2_000) { entered == "4711" }
    }

    @Test
    fun pinScreenSaysWhenItWasWrong() {
        compose.setContent { PinScreen(isNew = false, rejected = true, onEntered = {}, onBack = {}) }
        compose.onNodeWithTag("pin-wrong").assertExists()
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
            WriteScreen(Writing(set = box, index = 3), onGoTo = {
            }, onSkip = { skipped = true }, onRetry = {}, onOverwrite = {}, onSettings = {}, onDone = {}, log = null)
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
                Writing(
                    set = box,
                    index = 0,
                    status = WriteStatus.Already(WriteOutcome.AlreadyWritten(a, CardRecord("signdigital", "essen", "essen"))),
                ),
                onGoTo = {},
                onSkip = {},
                onRetry = {},
                onOverwrite = { overwrite = true },
                onSettings = {},
                onDone = {},
                log = null,
            )
        }
        compose.onNodeWithTag("overwrite").performSemanticsAction(SemanticsActions.OnClick)
        assert(overwrite)
    }

    @Test
    fun writeScreenSaysWhyAWriteFailedAndOffersAnotherGo() {
        var retried = false
        compose.setContent {
            WriteScreen(
                Writing(set = box, index = 0, status = WriteStatus.Failed(WriteOutcome.Failed(a, "Tag was lost"))),
                onGoTo = {},
                onSkip = {},
                onRetry = { retried = true },
                onOverwrite = {},
                onSettings = {},
                onDone = {},
                log = null,
            )
        }
        compose.onNodeWithTag("failed").assertExists()
        compose.onNodeWithText("Tag was lost").assertExists()
        compose.onNodeWithTag("retry").performSemanticsAction(SemanticsActions.OnClick)
        assert(retried)
    }

    @Test
    fun writeScreenShowsTheWordJustWrittenWhileTheBoxHasMovedOn() {
        compose.setContent {
            WriteScreen(
                Writing(
                    set = box,
                    index = 1,
                    status = WriteStatus.Done(WriteOutcome.Written(a, CardRecord("signdigital", "abend-s", "Abend(s)"))),
                ),
                onGoTo = {},
                onSkip = {},
                onRetry = {},
                onOverwrite = {},
                onSettings = {},
                onDone = {},
                log = null,
            )
        }
        compose.onNodeWithTag("word").assertTextEquals("Abend(s)")
        compose.onNodeWithTag("written").assertExists()
    }

    @Test
    fun writeScreenGoesBackToTheLists() {
        var back = false
        compose.setContent {
            WriteScreen(
                Writing(set = box, index = 2),
                onGoTo = {},
                onSkip = {},
                onRetry = {},
                onOverwrite = {},
                onSettings = {},
                onDone = {},
                log = null,
                onLists = { back = true },
            )
        }
        compose.onNodeWithTag("word").assertTextEquals("alle")
        compose.onNodeWithTag("set-title").assertTextEquals("Box")
        compose.onNodeWithTag("lists").performSemanticsAction(SemanticsActions.OnClick)
        assert(back)
    }

    @Test
    fun listsScreenOpensAStackAndSaysWhenThereIsNoLernliste() {
        var opened: String? = null
        val kita = CardSet("sd-L1", "Kita", listOf(ListCard("Jacke", "jacke", "s1"), ListCard("Mama", "mama", "s3")))
        compose.setContent {
            ListsScreen(
                lists = Lists(entries = listOf(ListEntry(kita))),
                onOpen = { opened = it },
                onReload = {},
                onSettings = {},
                onDone = {},
            )
        }
        compose.onNodeWithTag("lists-empty").assertDoesNotExist()
        compose.onNodeWithTag("list-sd-L1").performSemanticsAction(SemanticsActions.OnClick)
        assert(opened == "sd-L1")
    }

    @Test
    fun listsScreenWithoutLernlistenSaysWhereToMakeOne() {
        compose.setContent {
            ListsScreen(
                lists = Lists(),
                onOpen = {},
                onReload = {},
                onSettings = {},
                onDone = {},
            )
        }
        compose.onNodeWithTag("lists-empty").assertExists()
    }

    @Test
    fun listsScreenShowsHitsWhileSearchingAndOpensOne() {
        var opened: ListSign? = null
        val hit = ListSign("a1", "keineahnung", "keineAhnung", null)
        compose.setContent {
            ListsScreen(
                lists = Lists(entries = listOf(ListEntry(box))),
                onOpen = {},
                onReload = {},
                onSettings = {},
                onDone = {},
                search = Search(text = "ahnung", hits = listOf(hit)),
                counts = mapOf("keineahnung" to 2),
                onHit = { opened = it },
            )
        }
        compose.onNodeWithTag("list-sd-B").assertDoesNotExist()
        compose.onNodeWithText("2×").assertExists()
        compose.onNodeWithTag("hit-keineahnung").performSemanticsAction(SemanticsActions.OnClick)
        assert(opened == hit)
    }

    @Test
    fun deletingAListAsksFirst() {
        var deleted: String? = null
        compose.setContent {
            ListsScreen(
                lists = Lists(entries = listOf(ListEntry(box))),
                onOpen = {},
                onReload = {},
                onSettings = {},
                onDone = {},
                onDelete = { deleted = it },
            )
        }
        compose.onNodeWithTag("list-sd-B").performSemanticsAction(SemanticsActions.OnLongClick)
        compose.onNodeWithTag("delete-list").performSemanticsAction(SemanticsActions.OnClick)
        assert(deleted == null)
        compose.onNodeWithTag("delete-confirm").performSemanticsAction(SemanticsActions.OnClick)
        assert(deleted == "sd-B")
    }

    @Test
    fun aCardFromTheSearchOnceWrittenOffersItAgain() {
        var again = false
        val one = CardSet(AdultModel.SEARCH_SET, "", listOf(ListCard("keineAhnung", "keineahnung", "a1")))
        compose.setContent {
            WriteScreen(
                Writing(set = one, spent = true),
                onGoTo = {},
                onSkip = {},
                onRetry = {},
                onOverwrite = {},
                onSettings = {},
                onDone = {},
                log = null,
                search = Search(text = "ahnung"),
                onAgain = { again = true },
            )
        }
        compose.onNodeWithTag("written").assertExists()
        compose.onNodeWithTag("skip").assertDoesNotExist()
        compose.onNodeWithTag("search").assertExists()
        compose.onNodeWithTag("again").performSemanticsAction(SemanticsActions.OnClick)
        assert(again)
    }

    @Test
    fun theListPickerSaysWhereTheCardIsAlready() {
        var added: String? = null
        var removed: Pair<String, String>? = null
        val kita = CardSet("sd-K", "Kita", listOf(ListCard("Jacke", "jacke", "s1")))
        val other = CardSet("sd-H", "Halloween", listOf(ListCard("Kürbis", "kurbis", "s9")))
        compose.setContent {
            WriteScreen(
                Writing(set = kita),
                onGoTo = {},
                onSkip = {},
                onRetry = {},
                onOverwrite = {},
                onSettings = {},
                onDone = {},
                log = null,
                lists = Lists(entries = listOf(ListEntry(kita), ListEntry(other))),
                onAddTo = { added = it },
                onRemoveFrom = { list, sign -> removed = list to sign },
            )
        }
        compose.onNodeWithTag("add-to-list").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag("pick-sd-H").performSemanticsAction(SemanticsActions.OnClick)
        assert(added == "sd-H")
        compose.onNodeWithTag("pick-sd-K").performSemanticsAction(SemanticsActions.OnClick)
        assert(removed == ("sd-K" to "s1"))
    }
}
