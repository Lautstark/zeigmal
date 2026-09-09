package de.lautstark.zeigmal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.StationViewModel
import de.lautstark.zeigmal.cardset.Loaded
import de.lautstark.zeigmal.station.StationState

/**
 * The player screen claims the same exemption from the shared look that
 * wochenwerk's board does: it is a black surface with a video on it, and a
 * child never sees anything else. The diagnostics screen behind the long press
 * is for an adult with the phone in hand.
 */
@Composable
fun ZeigmalApp(model: StationViewModel) {
    val state by model.state.collectAsState()
    MaterialTheme {
        if (state.diagnostics) {
            BackHandler { model.toggleDiagnostics() }
            DiagnosticsScreen(state, model.directory, onClose = model::toggleDiagnostics)
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { model.toggleDiagnostics() }) },
            ) {
                when (val s = state.station) {
                    StationState.Idle -> {
                        IdleScreen(state.loaded, state.nfcAvailable, state.nfcEnabled)
                    }

                    is StationState.Unknown -> {
                        UnknownCard()
                    }

                    is StationState.Playing -> {
                        val loaded = state.loaded as? Loaded.Ready
                        if (loaded == null) {
                            IdleScreen(state.loaded, state.nfcAvailable, state.nfcEnabled)
                        } else {
                            SignVideo(
                                video = loaded.file(s.entry.video),
                                audio =
                                    s.entry.audio?.let { loaded.file(it.file) }?.takeIf {
                                        s.entry.speech ==
                                            de.lautstark.zeigmal.cardset.SpeechMode.EXTERNAL
                                    },
                                run = s.run,
                                onEnded = model::onPlaybackEnded,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleScreen(
    loaded: Loaded?,
    nfcAvailable: Boolean,
    nfcEnabled: Boolean,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.app_name), color = Color.White, fontSize = 40.sp)
        Text(stringResource(R.string.idle_hint), color = Color(0xFF9E9E9E), fontSize = 22.sp)
        val problem =
            when {
                !nfcAvailable -> stringResource(R.string.nfc_missing)
                !nfcEnabled -> stringResource(R.string.nfc_off)
                loaded is Loaded.Missing -> stringResource(R.string.no_kartensatz)
                loaded is Loaded.Rejected -> stringResource(R.string.kartensatz_refused, loaded.file)
                loaded is Loaded.Ready && loaded.cards.isEmpty() -> stringResource(R.string.no_cards)
                else -> null
            }
        if (problem != null) {
            Text(problem, color = Color(0xFF9E9E9E), fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp))
        }
    }
}

@Composable
private fun UnknownCard() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.unknown_card), color = Color(0xFF9E9E9E), fontSize = 24.sp)
    }
}
