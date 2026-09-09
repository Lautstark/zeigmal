package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.BuildConfig
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.UiState
import de.lautstark.zeigmal.cardset.Loaded
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What an adult needs with the phone in hand: is NFC on, which Kartensatz is
 * loaded and what was wrong with it, and every tag event with a timestamp — the
 * instrument for docs/experiments.md. Not a settings screen, and not an editor.
 */
@Composable
fun DiagnosticsScreen(
    state: UiState,
    directory: File,
    onClose: () -> Unit,
) {
    val grey = Color(0xFFBDBDBD)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .systemBarsPadding()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.diagnostics), color = Color.White, fontSize = 20.sp)
        Text("Zeigmal ${BuildConfig.VERSION_NAME}", color = grey, fontSize = 13.sp)
        Text(
            when {
                !state.nfcAvailable -> stringResource(R.string.nfc_missing)
                !state.nfcEnabled -> stringResource(R.string.nfc_off)
                else -> stringResource(R.string.nfc_ready)
            },
            color = grey,
            fontSize = 13.sp,
        )
        Text(directory.path, color = grey, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        when (val l = state.loaded) {
            null, is Loaded.Missing -> {
                Text(stringResource(R.string.no_kartensatz), color = grey, fontSize = 13.sp)
            }

            is Loaded.Rejected -> {
                Text("${l.file}: ${l.code} ${l.detail}", color = Color(0xFFFF8A80), fontSize = 13.sp)
            }

            is Loaded.Ready -> {
                Text(
                    stringResource(R.string.kartensatz_summary, l.set.name, l.set.entries.size, l.cards.size),
                    color = grey,
                    fontSize = 13.sp,
                )
                l.warnings.forEach { w ->
                    Text("${w.code}: ${w.detail}", color = Color(0xFFFFD180), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)
        LazyColumn(Modifier.weight(1f).padding(top = 12.dp)) {
            items(state.log.asReversed()) { line ->
                Text(
                    "${clock.format(Date(line.atMillis))}  ${line.text}",
                    color = Color(0xFFE0E0E0),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
    }
}
