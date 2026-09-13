package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.lautstark.zeigmal.R
import de.lautstark.zeigmal.StationViewModel
import de.lautstark.zeigmal.UiState

/** Once, when setting up. The login stays on this phone. */
@Composable
fun LoginScreen(
    state: UiState,
    model: StationViewModel,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val failed =
        state.log
            .lastOrNull()
            ?.text
            ?.startsWith("signdigital: Anmeldung fehlgeschlagen") == true
    Box(Modifier.fillMaxSize().background(Palette.bg), contentAlignment = Alignment.Center) {
        Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.login_title), color = Palette.text, fontSize = 24.sp)
            Text(stringResource(R.string.login_lead), color = Palette.textDim, fontSize = 15.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.signdigital_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.signdigital_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (failed) Text(stringResource(R.string.login_failed), color = Palette.danger, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = model::leaveAdult) { Text(stringResource(R.string.back)) }
                TextButton(onClick = model::toggleLog) { Text(stringResource(R.string.log)) }
                Button(
                    enabled = !state.busy && email.isNotBlank() && password.isNotBlank(),
                    onClick = { model.login(email, password) },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(stringResource(R.string.login)) }
            }
            if (state.showLog) LogPanel(state)
        }
    }
}

/** Light text on the dark ground; the theme's field defaults are for a light one. */
@Composable
fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Palette.text,
        unfocusedTextColor = Palette.text,
        cursorColor = Palette.text,
        focusedBorderColor = Palette.accentStrong,
        unfocusedBorderColor = Palette.line,
        focusedLabelColor = Palette.accentStrong,
        unfocusedLabelColor = Palette.textDim,
    )
