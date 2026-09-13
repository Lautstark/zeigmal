package de.lautstark.zeigmal

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.FakeProvider
import de.lautstark.zeigmal.core.FakeTagSource
import de.lautstark.zeigmal.core.InMemoryStore
import de.lautstark.zeigmal.core.Media
import de.lautstark.zeigmal.core.Phase
import de.lautstark.zeigmal.core.Provider
import de.lautstark.zeigmal.core.SignDigitalProvider
import de.lautstark.zeigmal.core.StationState
import de.lautstark.zeigmal.core.TagId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The whole path on a real device: a (fake) tag arrives, the station asks the
 * (fake) provider, Media3 plays a clip from the device's own storage, and the
 * first frame reaches the screen. The one thing this cannot do is put a
 * sticker on the antenna; that stays a row in docs/experiments.md.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val source = FakeTagSource()
    private lateinit var clip: File
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun fakes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        clip = File(context.cacheDir, "clip.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("clip.mp4").use { input ->
            clip.outputStream().use { input.copyTo(it) }
        }
        val store = InMemoryStore()
        Deps.store = { store }
        Deps.providers = { s ->
            mapOf<String, Provider>(
                SignDigitalProvider.ID to SignDigitalProvider(s, baseUrl = "http://localhost:1"),
                "fake" to FakeProvider(media = mapOf("trinken" to Media("file://${clip.path}", null))),
            )
        }
        Deps.tagSource = { source }
        Deps.pin = false
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun restore() {
        scenario.close()
        Deps.store = { context -> PreferencesStore(context) }
        Deps.pin = true
        Deps.providers = { s -> listOf<Provider>(SignDigitalProvider(s)).associateBy { it.id } }
        Deps.tagSource = { activity ->
            de.lautstark.zeigmal.nfc
                .AndroidTagSource(activity)
        }
    }

    private fun state(): StationState {
        var s: StationState = StationState.Idle
        scenario.onActivity {
            s =
                it
                    .viewModelForTest()
                    .station.state.value
        }
        return s
    }

    @Test
    fun aTagPlaysItsClipToTheFirstFrameAndBack() {
        val record = CardRecord("fake", "trinken", "trinken")
        val tag = TagId("04c5d4a88d2681")
        compose.onNodeWithTag("kid").assertExists()

        source.seen(tag, record)
        compose.waitUntil(5_000) { state().let { it is StationState.Card && it.phase == Phase.PLAYING } }

        // The card lies there: the clip loops, still PLAYING, no ring in between.
        Thread.sleep(3_000)
        assertEquals(Phase.PLAYING, (state() as StationState.Card).phase)

        // Card out: the loop that is running ends, no other begins, idle follows.
        source.gone(tag)
        compose.waitUntil(15_000) { state() == StationState.Idle }
        assertTrue(state() == StationState.Idle)
    }
}
