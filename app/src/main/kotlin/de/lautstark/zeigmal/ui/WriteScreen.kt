package de.lautstark.zeigmal.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import de.lautstark.zeigmal.LogLine
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.core.ListSign
import de.lautstark.zeigmal.core.Lists
import de.lautstark.zeigmal.core.Search
import de.lautstark.zeigmal.core.WriteStatus
import de.lautstark.zeigmal.core.Writing

/**
 * Writing a set, card by card. Left the box's order with what is done and
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
    onLists: () -> Unit = {},
    search: Search = Search(),
    lists: Lists = Lists(),
    onSearch: (String) -> Unit = {},
    onHit: (ListSign) -> Unit = {},
    onAddTo: (String) -> Unit = {},
    onRemoveFrom: (String, String) -> Unit = { _, _ -> },
    onAgain: () -> Unit = {},
) {
    val w = writing
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxSize()
            .background(Palette.bg)
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .testTag("write"),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (w.fromSearch) {
            SearchColumn(w, search, onSearch, onHit, onLists, Modifier.width(210.dp).fillMaxHeight())
        } else {
            WordList(w, onGoTo, onLists, onSettings, onRemoveFrom, Modifier.width(170.dp).fillMaxHeight())
        }
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
                    stringResource(R.string.sticker_already_hint, w.word?.label.orEmpty()),
                    color = Palette.textDim,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.next), maxLines = 1) }
                    Button(onClick = onOverwrite, modifier = Modifier.testTag("overwrite")) {
                        Text(stringResource(R.string.overwrite_as, w.word?.label.orEmpty()), maxLines = 1)
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
                    if (w.word?.id != null) {
                        TextButton(
                            onClick = { picking = true },
                            enabled = !w.busy && !lists.saving,
                            modifier = Modifier.testTag("add-to-list"),
                        ) {
                            Text(stringResource(R.string.add_to_list), color = Palette.textDim, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (w.spent) {
                        Button(
                            onClick = onAgain,
                            modifier = Modifier.testTag("again"),
                        ) { Text(stringResource(R.string.again), maxLines = 1) }
                    } else if (!w.fromSearch) {
                        TextButton(onClick = onSkip, enabled = !w.busy, modifier = Modifier.testTag("skip")) {
                            Text(
                                stringResource(R.string.skip),
                                color = if (w.busy) Palette.textFaint else Palette.accentStrong,
                                maxLines = 1,
                            )
                        }
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
            if (!w.fromSearch) {
                LinearProgressIndicator(
                    // Where in the stack, not how much is done: nothing keeps that.
                    progress = { (w.index + 1).toFloat() / w.total },
                    color = Palette.accentStrong,
                    trackColor = Palette.surface2,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp),
                )
            }
        }
    }
    val word = w.word
    val signId = word?.id
    if (picking && word != null && signId != null) {
        val holding = lists.holding(signId)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.add_to_list_title, word.label)) },
            text = {
                Column {
                    for (entry in lists.entries) {
                        val inIt = entry.set.id in holding
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !lists.saving) {
                                    if (inIt) onRemoveFrom(entry.set.id, signId) else onAddTo(entry.set.id)
                                }.padding(vertical = 12.dp)
                                .testTag("pick-${entry.set.id}"),
                        ) {
                            Text(entry.set.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(if (inIt) R.string.in_list else R.string.add),
                                color = if (inIt) Palette.accentStrong else Palette.textFaint,
                            )
                        }
                    }
                    if (lists.saving) Text(stringResource(R.string.saving), color = Palette.textFaint, fontSize = 13.sp)
                    lists.saveFailed?.let { Text(stringResource(R.string.save_failed, it), color = Palette.danger, fontSize = 13.sp) }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.done)) } },
        )
    }
}

/** The one line read while writing: what the sticker is up to, and in the colour of it. */
@Composable
private fun StatusLine(w: Writing) {
    val status = w.status
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp).fillMaxWidth()) {
        when {
            status is WriteStatus.Busy -> {
                CircularProgressIndicator(
                    color = Palette.accentStrong,
                    trackColor = Palette.accentSoft,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.writing_busy), color = Palette.text, fontSize = 20.sp, modifier = Modifier.testTag("busy"))
            }

            // A card from the search stays "written" until the next one is chosen.
            status is WriteStatus.Done || w.spent -> {
                Text("✓", color = Palette.accentStrong, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.written_ok),
                    color = Palette.accentStrong,
                    fontSize = 20.sp,
                    modifier = Modifier.testTag("written"),
                )
            }

            status is WriteStatus.Failed -> {
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
        when {
            status is WriteStatus.Busy -> stringResource(R.string.writing_busy_hint)
            status is WriteStatus.Done && w.fromSearch -> "${status.outcome.tag} · " + stringResource(R.string.spent_hint)
            status is WriteStatus.Done -> "${status.outcome.tag} · " + stringResource(R.string.next_up, w.word?.label.orEmpty())
            status is WriteStatus.Failed -> status.outcome.reason
            w.spent -> stringResource(R.string.spent_hint)
            else -> null
        }
    if (sub != null) {
        Text(sub, color = Palette.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 26.dp, top = 2.dp))
    }
}

/** Beside a card from the search: the search itself, so the next card is a word away. */
@Composable
private fun SearchColumn(
    w: Writing,
    search: Search,
    onSearch: (String) -> Unit,
    onHit: (ListSign) -> Unit,
    onLists: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "‹ " + stringResource(R.string.lists),
            color = if (w.busy) Palette.textFaint else Palette.textDim,
            fontSize = 13.sp,
            maxLines = 1,
            modifier =
                Modifier
                    .clickable(enabled = !w.busy, onClick = onLists)
                    .padding(vertical = 4.dp)
                    .testTag("lists"),
        )
        SearchField(search.text, onSearch, Modifier.fillMaxWidth())
        Hits(search, w.counts, { if (!w.busy) onHit(it) }, Modifier.weight(1f), chosen = w.word?.ref)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordList(
    w: Writing,
    onGoTo: (Int) -> Unit,
    onLists: () -> Unit,
    onSettings: () -> Unit,
    onRemoveFrom: (String, String) -> Unit,
    modifier: Modifier,
) {
    val list = rememberLazyListState()
    var menuAt by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(w.index) { list.animateScrollToItem((w.index - 3).coerceAtLeast(0)) }
    Column(modifier) {
        // Back to the lists, and the one being written. Still while the reader writes.
        Text(
            "‹ " + stringResource(R.string.lists),
            color = if (w.busy) Palette.textFaint else Palette.textDim,
            fontSize = 13.sp,
            maxLines = 1,
            modifier =
                Modifier
                    .clickable(enabled = !w.busy, onClick = onLists)
                    .padding(vertical = 4.dp)
                    .testTag("lists"),
        )
        Text(
            w.set.title,
            color = Palette.accentStrong,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("set-title"),
        )
        Text(
            "${w.index + 1} von ${w.total}",
            color = Palette.textFaint,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        LazyColumn(state = list, modifier = Modifier.weight(1f)) {
            itemsIndexed(w.set.words) { i, word ->
                val count = w.count(word)
                val current = i == w.index
                val next = i == w.index + 1
                // A long press takes the card out of the list, on the site.
                Box {
                    DropdownMenu(expanded = menuAt == i, onDismissRequest = { menuAt = null }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_from, w.set.title), color = Palette.danger) },
                            onClick = {
                                menuAt = null
                                word.id?.let { onRemoveFrom(w.set.id, it) }
                            },
                            modifier = Modifier.testTag("remove-${word.ref}"),
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (current) Palette.accentSoft else Color.Transparent)
                        .combinedClickable(
                            enabled = !w.busy,
                            onClick = { onGoTo(i) },
                            onLongClick = { if (word.id != null) menuAt = i },
                        ).padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("word-$i"),
                ) {
                    Text(
                        word.label,
                        color =
                            if (current) {
                                Palette.accentStrong
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

                            // How often this sign has gone on a sticker, from any list: a card the family has twice counts twice.
                            count > 0 -> "$count×"

                            else -> ""
                        },
                        color = if (count > 0 && !current && !next) Palette.accentStrong else Palette.textFaint,
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
