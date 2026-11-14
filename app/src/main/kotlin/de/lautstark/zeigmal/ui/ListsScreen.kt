package de.lautstark.zeigmal.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.core.ListEntry
import de.lautstark.zeigmal.core.ListSign
import de.lautstark.zeigmal.core.Lists
import de.lautstark.zeigmal.core.Search

/**
 * What the adult mode opens on: the account's Lernlisten, newest first, each
 * with how many cards it holds, and the search above them. A tap opens the
 * writing mode on that stack; a long press offers to delete it. Typing shows
 * what the site finds instead of the lists, and a hit is written on its own.
 */
@Composable
fun ListsScreen(
    lists: Lists,
    onOpen: (String) -> Unit,
    onReload: () -> Unit,
    onSettings: () -> Unit,
    onDone: () -> Unit,
    search: Search = Search(),
    counts: Map<String, Int> = emptyMap(),
    onSearch: (String) -> Unit = {},
    onHit: (ListSign) -> Unit = {},
    onDelete: (String) -> Unit = {},
) {
    var asked by remember { mutableStateOf<ListEntry?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.bg)
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 18.dp)
            .testTag("lists-screen"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mark(Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.lists_title), color = Palette.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(14.dp))
            SearchField(search.text, onSearch, Modifier.weight(1f))
            TextButton(onClick = onReload, enabled = !lists.loading, modifier = Modifier.testTag("lists-reload")) {
                Text("↻ " + stringResource(R.string.lists_reload), color = Palette.textDim, maxLines = 1)
            }
            TextButton(onClick = onDone, modifier = Modifier.testTag("done")) {
                Text(stringResource(R.string.done), color = Palette.accentStrong, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Text(
            when {
                lists.saving -> stringResource(R.string.saving)
                lists.saveFailed != null -> stringResource(R.string.save_failed, lists.saveFailed.orEmpty())
                lists.loading -> stringResource(R.string.lists_loading)
                lists.failed != null -> stringResource(R.string.lists_failed)
                else -> ""
            },
            color =
                if ((lists.saveFailed != null || lists.failed != null) && !lists.loading &&
                    !lists.saving
                ) {
                    Palette.danger
                } else {
                    Palette.textFaint
                },
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp).testTag("lists-status"),
        )
        if (search.text.isNotBlank()) {
            Hits(search, counts, onHit, Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lists.entries.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 18.dp).testTag("lists-empty"), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.lists_empty),
                                color = Palette.textDim,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                items(lists.entries, key = { it.set.id }) { entry ->
                    ListRow(entry, onOpen, onDeleteAsked = { asked = entry }, enabled = !lists.saving)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSettings, modifier = Modifier.testTag("settings")) {
                Text("⚙  " + stringResource(R.string.settings), color = Palette.textFaint, fontSize = 13.sp, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.lists_made_on_site), color = Palette.textFaint, fontSize = 12.sp, maxLines = 1)
        }
    }
    asked?.let { entry ->
        AlertDialog(
            onDismissRequest = { asked = null },
            title = { Text(stringResource(R.string.delete_title, entry.set.title)) },
            text = { Text(stringResource(R.string.delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        asked = null
                        onDelete(entry.set.id)
                    },
                    modifier = Modifier.testTag("delete-confirm"),
                ) { Text(stringResource(R.string.delete), color = Palette.danger) }
            },
            dismissButton = { TextButton(onClick = { asked = null }) { Text(stringResource(R.string.keep)) } },
        )
    }
}

/** The search field, in the app's colours; also used beside a card from the search. */
@Composable
fun SearchField(
    text: String,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = text,
        onValueChange = onSearch,
        singleLine = true,
        textStyle = TextStyle(color = Palette.text, fontSize = 16.sp),
        cursorBrush = SolidColor(Palette.accentStrong),
        modifier = modifier.testTag("search"),
        decorationBox = { field ->
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Palette.surface)
                    .border(1.dp, if (text.isEmpty()) Palette.line else Palette.accentStrong, RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        "⌕  " + stringResource(R.string.search_hint),
                        color = Palette.textDim,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                }
                field()
            }
        },
    )
}

/** What the site found, each with how often it is on a sticker already. */
@Composable
fun Hits(
    search: Search,
    counts: Map<String, Int>,
    onHit: (ListSign) -> Unit,
    modifier: Modifier = Modifier,
    chosen: String? = null,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (search.hits.isEmpty() && !search.loading) {
            item {
                Text(
                    stringResource(if (search.failed != null) R.string.search_failed else R.string.search_none),
                    color = if (search.failed != null) Palette.danger else Palette.textDim,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(8.dp).testTag("search-none"),
                )
            }
        }
        items(search.hits, key = { it.id }) { sign ->
            val count = counts[sign.slug] ?: 0
            val current = sign.slug == chosen
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (current) Palette.accentSoft else Palette.surface)
                    .clickable { onHit(sign) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("hit-${sign.slug}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    sign.name,
                    color = if (current) Palette.accentStrong else Palette.text,
                    fontSize = 16.sp,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (count > 0) Text("$count×", color = Palette.accentStrong, fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListRow(
    entry: ListEntry,
    onOpen: (String) -> Unit,
    onDeleteAsked: () -> Unit,
    enabled: Boolean,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.surface)
                .combinedClickable(enabled = enabled, onClick = { onOpen(entry.set.id) }, onLongClick = { menu = true })
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("list-${entry.set.id}"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.set.title,
                    color = Palette.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    pluralStringResource(R.plurals.lists_cards, entry.total, entry.total),
                    color = Palette.textDim,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_list), color = Palette.danger) },
                onClick = {
                    menu = false
                    onDeleteAsked()
                },
                modifier = Modifier.testTag("delete-list"),
            )
        }
    }
}
