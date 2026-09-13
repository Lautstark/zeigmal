package de.lautstark.zeigmal

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import de.lautstark.zeigmal.nfc.NfcReader
import de.lautstark.zeigmal.ui.ZeigmalApp
import kotlinx.coroutines.launch

/**
 * The one activity. Landscape comes from the manifest; the screen stays on for
 * as long as this is in front; the system bars are hidden, not locked — screen
 * pinning is a setting a person turns on, see docs/hardware.md.
 */
class MainActivity : ComponentActivity() {
    private val model: StationViewModel by viewModels()
    private lateinit var reader: NfcReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        reader =
            NfcReader(
                this,
                onTag = model::onTag,
                onWrite = { outcome ->
                    model.overwriteDone()
                    model.onWrite(outcome)
                },
            )
        // The reader writes whatever the writing mode has up, and reads otherwise.
        lifecycleScope.launch {
            model.state.collect { s ->
                reader.pendingWrite = model.pendingRecord
                reader.overwrite = model.overwriteNext
            }
        }
        setContent { ZeigmalApp(model) }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        model.nfcStatus(reader.available, reader.enabled)
        reader.start()
    }

    override fun onPause() {
        reader.stop()
        super.onPause()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
