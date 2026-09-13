package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.core.Pin

/** Four digits between the child's screen and the adult's. The first four ever typed become the PIN. */
@Composable
fun PinScreen(
    isNew: Boolean,
    rejected: Boolean,
    onEntered: (String) -> Unit,
    onBack: () -> Unit,
) {
    var digits by remember { mutableStateOf("") }
    LaunchedEffect(rejected) { if (rejected) digits = "" }
    LaunchedEffect(digits) {
        if (digits.length == Pin.LENGTH) {
            onEntered(digits)
            digits = ""
        }
    }
    Box(Modifier.fillMaxSize().background(Palette.bg).testTag("pin"), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(if (isNew) R.string.pin_choose else R.string.pin_enter), color = Palette.text, fontSize = 20.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(Pin.LENGTH) { i ->
                        Box(
                            Modifier.size(16.dp).clip(CircleShape).background(
                                if (i <
                                    digits.length
                                ) {
                                    Palette.accentStrong
                                } else {
                                    Palette.surface2
                                },
                            ),
                        )
                    }
                }
                if (rejected) {
                    Text(
                        stringResource(R.string.pin_wrong),
                        color = Palette.danger,
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("pin-wrong"),
                    )
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (key.isEmpty()) Palette.bg else Palette.surface2)
                                    .clickable(enabled = key.isNotEmpty()) {
                                        digits = if (key == "⌫") digits.dropLast(1) else (digits + key).take(Pin.LENGTH)
                                    }.testTag("key-$key"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(key, color = Palette.text, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
