package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.core.ListEntry
import de.lautstark.zeigmal.core.Lists

/**
 * What the adult mode opens on: the account's Lernlisten, newest first, then
 * the sets still in the app, each with how many cards it holds. A tap opens
 * the writing mode on that stack. Nothing here changes a
 * list: they are made on sign-digital.de.
 */
@Composable
fun ListsScreen(
    lists: Lists,
    onOpen: (String) -> Unit,
    onReload: () -> Unit,
    onSettings: () -> Unit,
    onDone: () -> Unit,
) {
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
            Text(
                when {
                    lists.loading -> stringResource(R.string.lists_loading)
                    lists.failed != null -> stringResource(R.string.lists_failed)
                    else -> ""
                },
                color = if (lists.failed != null && !lists.loading) Palette.danger else Palette.textFaint,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("lists-status"),
            )
            TextButton(onClick = onReload, enabled = !lists.loading, modifier = Modifier.testTag("lists-reload")) {
                Text("↻ " + stringResource(R.string.lists_reload), color = Palette.textDim, maxLines = 1)
            }
            TextButton(onClick = onDone, modifier = Modifier.testTag("done")) {
                Text(stringResource(R.string.done), color = Palette.accentStrong, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        val site = lists.entries.take(lists.fromSite)
        val inApp = lists.entries.drop(lists.fromSite)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (site.isEmpty()) {
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
            items(site, key = { it.set.id }) { ListRow(it, onOpen) }
            if (inApp.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.lists_in_app).uppercase(),
                        color = Palette.textFaint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                items(inApp, key = { it.set.id }) { ListRow(it, onOpen) }
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
}

@Composable
private fun ListRow(
    entry: ListEntry,
    onOpen: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Palette.surface)
            .clickable { onOpen(entry.set.id) }
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
}
