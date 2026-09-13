package de.lautstark.zeigmal.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.LogLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The reader's and the station's events with timestamps: the instrument for docs/experiments.md. */
@Composable
fun LogPanel(log: List<LogLine>) {
    val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)
    LazyColumn(Modifier.fillMaxWidth()) {
        items(log.asReversed()) { line ->
            Text(
                "${clock.format(Date(line.atMillis))}  ${line.text}",
                color = Palette.textDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
