package de.lautstark.zeigmal.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import de.lautstark.zeigmal.core.WriteStatus
import de.lautstark.zeigmal.core.Writing

/**
 * Writing the box, card by card. Left the box's order with what is done and
 * what comes next; middle the card as SIGNdigital shows it, so the right one
 * comes out of the stack; right the word and, right under it, the one line
 * that says what the sticker is up to. The card's border says the same in
 * colour, for a glance from across the table.
 */
@Composable
fun WriteScreen(
    writing: Writing,
    onGoTo: (Int) -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
    onOverwrite: () -> Unit,
    onSettings: () -> Unit,
    onDone: () -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        WordList(w, onGoTo, onSettings, Modifier.width(170.dp).fillMaxHeight())
        CardImage(w.cardImageUrl, w.lookupFailed, w.status, Modifier.fillMaxHeight(0.92f).aspectRatio(2f / 3f))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mark(Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.app_name).lowercase(),
                    color = Palette.textDim,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDone, enabled = !w.busy, modifier = Modifier.testTag("done")) {
                    Text(
                        stringResource(R.string.done),
                        color = if (w.busy) Palette.textFaint else Palette.accentStrong,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            val status = w.status
            if (status is WriteStatus.Already) {
                val outcome = status.outcome
                Text(
                    stringResource(R.string.sticker_already, outcome.record.label),
                    color = Palette.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 32.sp,
                )
                Text(
                    "${outcome.record.provider} / ${outcome.record.ref} · ${outcome.tag}",
                    color = Palette.textFaint,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    stringResource(R.string.sticker_already_hint, w.word.label),
                    color = Palette.textDim,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.next), maxLines = 1) }
                    Button(onClick = onOverwrite, modifier = Modifier.testTag("overwrite")) {
                        Text(stringResource(R.string.overwrite_as, w.word.label), maxLines = 1)
                    }
                }
            } else {
                Text(
                    w.shownLabel,
                    color = Palette.text,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 44.sp,
                    modifier = Modifier.padding(top = 4.dp).testTag("word"),
                )
                StatusLine(w)
                if (log != null) {
                    Box(Modifier.weight(1f).padding(top = 8.dp)) { LogPanel(log) }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSkip, enabled = !w.busy, modifier = Modifier.testTag("skip")) {
                        Text(stringResource(R.string.skip), color = if (w.busy) Palette.textFaint else Palette.accentStrong, maxLines = 1)
                    }
                    if (status is WriteStatus.Failed) {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag("retry"),
                        ) { Text(stringResource(R.string.retry), maxLines = 1) }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { w.written.size.toFloat() / w.total },
                color = Palette.accentStrong,
                trackColor = Palette.surface2,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp),
            )
        }
    }
}

/** The one line read while writing: what the sticker is up to, and in the colour of it. */
@Composable
private fun StatusLine(w: Writing) {
    val status = w.status
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp).fillMaxWidth()) {
        when (status) {
            is WriteStatus.Busy -> {
                CircularProgressIndicator(
                    color = Palette.accentStrong,
                    trackColor = Palette.accentSoft,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.writing_busy), color = Palette.text, fontSize = 20.sp, modifier = Modifier.testTag("busy"))
            }

            is WriteStatus.Done -> {
                Text("✓", color = Palette.accentStrong, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.written_ok),
                    color = Palette.accentStrong,
                    fontSize = 20.sp,
                    modifier = Modifier.testTag("written"),
                )
            }

            is WriteStatus.Failed -> {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(7.dp)).background(Palette.danger))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.write_failed), color = Palette.danger, fontSize = 20.sp, modifier = Modifier.testTag("failed"))
            }

            else -> {
                // The dot breathes: the reader is on, and nothing lies there yet.
                val breath = rememberInfiniteTransition(label = "waiting")
                val alpha by breath.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
                Box(
                    Modifier
                        .size(14.dp)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Palette.accentStrong),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.waiting_for_sticker), color = Palette.text, fontSize = 20.sp)
            }
        }
    }
    // The small print under the line: what to do, or what went wrong.
    val sub =
        when (status) {
            is WriteStatus.Busy -> stringResource(R.string.writing_busy_hint)
            is WriteStatus.Done -> "${status.outcome.tag} · " + stringResource(R.string.next_up, w.word.label)
            is WriteStatus.Failed -> status.outcome.reason
            else -> null
        }
    if (sub != null) {
        Text(sub, color = Palette.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 26.dp, top = 2.dp))
    }
}

@Composable
private fun WordList(
    w: Writing,
    onGoTo: (Int) -> Unit,
    onSettings: () -> Unit,
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
        // The gear at the foot of the list: the one way to the settings.
        TextButton(onClick = onSettings, modifier = Modifier.padding(top = 4.dp).testTag("settings")) {
            Text("⚙  " + stringResource(R.string.settings), color = Palette.textFaint, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CardImage(
    url: String?,
    failed: String?,
    status: WriteStatus,
    modifier: Modifier,
) {
    // The border says what the line says, in colour alone.
    val edge =
        when (status) {
            is WriteStatus.Busy, is WriteStatus.Done -> Palette.accentStrong
            is WriteStatus.Failed -> Palette.danger
            else -> Color.Transparent
        }
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).border(3.dp, edge, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
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
