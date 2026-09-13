package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.LogLine
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.core.Login
import de.lautstark.zeigmal.core.Settings
import de.lautstark.zeigmal.core.SettingsValues

/** The few knobs: how often a card plays, the PIN, the login, the log. */
@Composable
fun SettingsScreen(
    values: SettingsValues,
    login: Login,
    onMaxLoops: (Int) -> Unit,
    onChangePin: () -> Unit,
    onRelogin: () -> Unit,
    onToggleLog: () -> Unit,
    onClose: () -> Unit,
    log: List<LogLine>?,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.bg)
            .systemBarsPadding()
            .testTag("settings-screen"),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings), color = Palette.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose, modifier = Modifier.testTag("settings-close")) {
                    Text(stringResource(R.string.done), color = Palette.accentStrong, fontWeight = FontWeight.SemiBold)
                }
            }
            Setting(stringResource(R.string.setting_loops), stringResource(R.string.setting_loops_hint)) {
                TextButton(
                    onClick = { onMaxLoops(values.maxLoops - 1) },
                    enabled = values.maxLoops > Settings.MIN_LOOPS,
                ) { Text("−", fontSize = 20.sp) }
                Text(
                    "${values.maxLoops}",
                    color = Palette.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(48.dp).testTag("loops"),
                )
                TextButton(
                    onClick = { onMaxLoops(values.maxLoops + 1) },
                    enabled = values.maxLoops < Settings.MAX_LOOPS,
                ) { Text("+", fontSize = 20.sp) }
            }
            Setting(stringResource(R.string.setting_pin), stringResource(R.string.setting_pin_hint)) {
                TextButton(onClick = onChangePin, modifier = Modifier.testTag("change-pin")) { Text(stringResource(R.string.change)) }
            }
            Setting(
                stringResource(R.string.login_title),
                when (login) {
                    is Login.In -> login.email
                    else -> stringResource(R.string.setting_not_logged_in)
                },
            ) {
                TextButton(onClick = onRelogin, modifier = Modifier.testTag("relogin")) {
                    Text(stringResource(if (login is Login.In) R.string.relogin else R.string.login))
                }
            }
            Setting(stringResource(R.string.log), stringResource(R.string.setting_log_hint)) {
                TextButton(onClick = onToggleLog) { Text(stringResource(if (log == null) R.string.show else R.string.hide)) }
            }
            if (log != null) Box(Modifier.weight(1f)) { LogPanel(log) }
        }
    }
}

@Composable
private fun Setting(
    title: String,
    hint: String,
    controls: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(hint, color = Palette.textDim, fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { controls() }
    }
}
