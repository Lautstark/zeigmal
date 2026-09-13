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
import de.lautstark.zeigmal.core.TagSource
import de.lautstark.zeigmal.nfc.AndroidTagSource
import de.lautstark.zeigmal.ui.ZeigmalApp

/**
 * The one activity. Landscape comes from the manifest; the screen stays on for
 * as long as this is in front; the system bars are hidden, not locked — screen
 * pinning is a setting a person turns on, see docs/hardware.md.
 */
class MainActivity : ComponentActivity() {
    private val model: ZeigmalViewModel by viewModels {
        val store = Deps.store(applicationContext)
        ZeigmalViewModel.Factory(store, Deps.providers(store))
    }
    private lateinit var tags: TagSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tags = Deps.tagSource(this)
        model.attach(tags)
        setContent { ZeigmalApp(model) }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        (tags as? AndroidTagSource)?.start()
    }

    override fun onPause() {
        (tags as? AndroidTagSource)?.stop()
        super.onPause()
    }

    override fun onDestroy() {
        model.detach()
        super.onDestroy()
    }

    /** For the instrumented end-to-end test, which watches the station from outside. */
    fun viewModelForTest(): ZeigmalViewModel = model

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
