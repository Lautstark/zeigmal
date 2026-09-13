package de.lautstark.zeigmal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import de.lautstark.zeigmal.Mode
import de.lautstark.zeigmal.ZeigmalViewModel

@Composable
fun ZeigmalApp(model: ZeigmalViewModel) {
    val mode by model.mode.collectAsState()
    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Palette.accent,
                onPrimary = Color.White,
                background = Palette.bg,
                surface = Palette.surface,
                onSurface = Palette.text,
                onBackground = Palette.text,
            ),
    ) {
        when (mode) {
            Mode.KID -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .testTag("kid")
                        .pointerInput(Unit) { detectTapGestures(onLongPress = { model.enterAdult() }) },
                ) {
                    val station by model.station.state.collectAsState()
                    KidScreen(
                        station = station,
                        videoUrl = model.station::videoUrl,
                        onFirstFrame = model.station::onFirstFrame,
                        onEnded = model.station::onPlaybackEnded,
                        onFailed = model.station::onPlaybackFailed,
                    )
                }
            }

            Mode.LOGIN -> {
                BackHandler { model.leaveAdult() }
                val login by model.adult.login.collectAsState()
                val showLog by model.showLog.collectAsState()
                val log by model.log.collectAsState()
                LoginScreen(
                    login = login,
                    onLogin = model.adult::login,
                    onBack = model::leaveAdult,
                    onToggleLog = model::toggleLog,
                    log = if (showLog) log else null,
                )
            }

            Mode.WRITE -> {
                BackHandler { model.leaveAdult() }
                val writing by model.adult.writing.collectAsState()
                val showLog by model.showLog.collectAsState()
                val log by model.log.collectAsState()
                WriteScreen(
                    writing = writing,
                    onGoTo = model.adult::goTo,
                    onSkip = model.adult::skip,
                    onBack = model.adult::back,
                    onOverwrite = model.adult::overwriteNext,
                    onLogout = model.adult::logout,
                    onToggleLog = model::toggleLog,
                    onDone = model::leaveAdult,
                    log = if (showLog) log else null,
                )
            }
        }
    }
}
