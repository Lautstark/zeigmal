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
import de.lautstark.zeigmal.Mode
import de.lautstark.zeigmal.StationViewModel

@Composable
fun ZeigmalApp(model: StationViewModel) {
    val state by model.state.collectAsState()
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
        when (state.mode) {
            Mode.KID -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) { detectTapGestures(onLongPress = { model.enterAdult() }) },
                ) {
                    KidScreen(state, model)
                }
            }

            Mode.LOGIN -> {
                BackHandler { model.leaveAdult() }
                LoginScreen(state, model)
            }

            Mode.WRITE -> {
                BackHandler { model.leaveAdult() }
                WriteScreen(state, model)
            }
        }
    }
}
