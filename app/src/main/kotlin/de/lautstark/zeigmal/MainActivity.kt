package de.lautstark.zeigmal

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
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
        // A station shows itself over the lock screen and turns the screen on:
        // after a reboot or a dark screen, the app is what is there, not a
        // swipe-to-unlock nobody in the kitchen will do.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        tags = Deps.tagSource(this)
        model.attach(tags)
        setContent { ZeigmalApp(model) }
        devLogin(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        devLogin(intent)
    }

    /**
     * Debug builds only: the login handed over from the laptop, so that a
     * developer reinstalling the app twenty times a day does not type it
     * twenty times. `tools/dev-login.sh` sends it; a release build ignores it.
     */
    private fun devLogin(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        val email = intent.getStringExtra(EXTRA_EMAIL) ?: return
        val password = intent.getStringExtra(EXTRA_PASSWORD) ?: return
        intent.removeExtra(EXTRA_PASSWORD)
        model.adult.login(email, password)
        model.enterAdult()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        // Android switches NFC polling off while the keyguard is up, even with
        // this activity showing over it (NfcService: "Screen State: ON_LOCKED",
        // seen 2026-09-13). On a swipe lock this dismisses it without input; on a
        // PIN it asks once. docs/hardware.md says why a station has no PIN.
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard.isKeyguardLocked) {
            keyguard.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = hideSystemBars()
                },
            )
        }
        (tags as? AndroidTagSource)?.start()
        pin()
    }

    /**
     * Screen pinning, release builds only: Home and Recents do nothing, the
     * notification shade stays closed. It is Android's pinned mode, not a lock
     * (Back+Overview held together still leaves), and it needs the person to
     * turn "Apps anheften" on once and confirm the first pin; it is off in
     * debug builds because that dialog would block every test run.
     */
    private fun pin() {
        if (BuildConfig.DEBUG) return
        val activityManager = getSystemService(ActivityManager::class.java)
        if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
            runCatching { startLockTask() }
        }
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

    private companion object {
        const val EXTRA_EMAIL = "de.lautstark.zeigmal.dev.email"
        const val EXTRA_PASSWORD = "de.lautstark.zeigmal.dev.password"
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
