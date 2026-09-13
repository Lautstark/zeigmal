package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.lautstark.zeigmal.LogLine
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.core.SignBox
import de.lautstark.zeigmal.core.WriteOutcome
import de.lautstark.zeigmal.core.Writing

/**
 * Writing the box, card by card. Left the box's order with what is done and
 * what comes next; middle the card as SIGNdigital shows it, so the right one
 * comes out of the stack; right the word, three steps and the status line.
 */
@Composable
fun WriteScreen(
    writing: Writing,
    onGoTo: (Int) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onOverwrite: () -> Unit,
    onLogout: () -> Unit,
    onToggleLog: () -> Unit,
    log: List<LogLine>?,
) {
    val w = writing
    Row(
        Modifier
            .fillMaxSize()
            .background(Palette.bg)
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .testTag("write"),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        WordList(w, onGoTo, Modifier.width(210.dp).fillMaxHeight())
        CardImage(w.cardImageUrl, w.lookupFailed, Modifier.fillMaxHeight().aspectRatio(2f / 3f))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mark(Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.app_name).lowercase(),
                    color = Palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onToggleLog) { Text(stringResource(R.string.log), color = Palette.textFaint) }
                TextButton(onClick = onLogout) { Text(stringResource(R.string.logout), color = Palette.textFaint) }
            }
            val outcome = w.lastOutcome
            if (outcome is WriteOutcome.AlreadyWritten) {
                Text(
                    stringResource(R.string.sticker_already, outcome.record.label),
                    color = Palette.text,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${outcome.record.provider} / ${outcome.record.ref} · ${outcome.tag}",
                    color = Palette.textFaint,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    stringResource(R.string.sticker_already_hint, w.word.label),
                    color = Palette.textDim,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.next)) }
                    Button(
                        onClick = onOverwrite,
                        modifier = Modifier.testTag("overwrite"),
                    ) { Text(stringResource(R.string.overwrite_as, w.word.label)) }
                }
            } else {
                Text(
                    w.word.label,
                    color = Palette.text,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 56.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text("signdigital / ${w.word.ref}", color = Palette.textFaint, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.step_1), color = Palette.textDim, fontSize = 17.sp)
                    Text(stringResource(R.string.step_2), color = Palette.textDim, fontSize = 17.sp)
                    Text(stringResource(R.string.step_3), color = Palette.textDim, fontSize = 17.sp)
                }
                Spacer(Modifier.weight(1f))
                if (log != null) LogPanel(log)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (outcome) {
                        is WriteOutcome.Written -> {
                            Text(
                                "✓ „${outcome.record.label}“ · ${outcome.tag}",
                                color = Palette.accentStrong,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        is WriteOutcome.Failed -> {
                            Text(stringResource(R.string.write_failed, outcome.reason), color = Palette.danger, fontSize = 15.sp)
                        }

                        else -> {
                            Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Palette.accentStrong))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.waiting_for_sticker), color = Palette.textDim, fontSize = 17.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                    TextButton(onClick = onSkip, modifier = Modifier.testTag("skip")) { Text(stringResource(R.string.skip)) }
                }
            }
            LinearProgressIndicator(
                progress = { w.written.size.toFloat() / w.total },
                color = Palette.accentStrong,
                trackColor = Palette.surface2,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(3.dp),
            )
        }
    }
}

@Composable
private fun WordList(
    w: Writing,
    onGoTo: (Int) -> Unit,
    modifier: Modifier,
) {
    val list = rememberLazyListState()
    LaunchedEffect(w.index) { list.animateScrollToItem((w.index - 3).coerceAtLeast(0)) }
    Column(modifier) {
        Row(Modifier.padding(bottom = 6.dp)) {
            Text("SIGNBOX 1", color = Palette.textFaint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${w.index + 1} von ${w.total}", color = Palette.textFaint, fontSize = 12.sp)
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f)) {
            itemsIndexed(SignBox.box1) { i, word ->
                val done = word.ref in w.written
                val current = i == w.index
                val next = i == w.index + 1
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (current) Palette.accentSoft else Color.Transparent)
                        .clickable { onGoTo(i) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        word.label,
                        color =
                            if (current) {
                                Palette.accentStrong
                            } else if (done) {
                                Palette.textFaint
                            } else if (next) {
                                Palette.text
                            } else {
                                Palette.textDim
                            },
                        fontSize = 15.sp,
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            current -> stringResource(R.string.now)
                            next -> stringResource(R.string.then)
                            done -> "✓"
                            else -> ""
                        },
                        color = if (done && !current) Palette.accentStrong else Palette.textFaint,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Text("${w.written.size} ✓", color = Palette.textFaint, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun CardImage(
    url: String?,
    failed: String?,
    modifier: Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
        when {
            url != null -> {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            failed != null -> {
                Text(
                    stringResource(R.string.card_not_found),
                    color = Color(0xFF999999),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }

            else -> {
                Text("…", color = Color(0xFF999999), fontSize = 24.sp)
            }
        }
    }
}
