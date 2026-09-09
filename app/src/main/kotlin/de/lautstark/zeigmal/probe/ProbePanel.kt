package de.lautstark.zeigmal.probe

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import de.lautstark.zeigmal.R
import kotlinx.coroutines.launch
import java.io.File

/**
 * Two buttons and a tiny player. "Stream" times the public sign from the
 * network; "Datei" times a local file for comparison. Both report the moment
 * the first frame is on the surface, measured from the tap, which stands in
 * for "card seen". Add the NFC latency from E1/S1 to get card-to-picture.
 */
@Composable
fun ProbePanel(localVideo: File?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateListOf<String>() }
    var status by remember { mutableStateOf("") }
    val player = remember { ExoPlayer.Builder(context).build() }
    var startedAt by remember { mutableStateOf(0L) }
    var prefix by remember { mutableStateOf("") }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    val ms = (System.nanoTime() - startedAt) / 1_000_000
                    results.add(0, "$prefix erstes Bild nach $ms ms")
                    status = ""
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    fun play(
        uri: Uri,
        label: String,
    ) {
        prefix = label
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    Column(Modifier.padding(vertical = 8.dp)) {
        Text("S11 · Stream gegen Datei", color = Color.White, fontSize = 14.sp)
        Row {
            TextButton(onClick = {
                status = "hole Link …"
                startedAt = System.nanoTime()
                scope.launch {
                    try {
                        val link = StreamProbe.signedLink()
                        play(link.url.toUri(), "stream: Suche ${link.lookupMs} ms, Link ${link.linkMs} ms,")
                    } catch (e: Exception) {
                        results.add(0, "stream: Fehler ${e.message}")
                        status = ""
                    }
                }
            }) { Text("Stream") }
            Spacer(Modifier.width(8.dp))
            TextButton(
                enabled = localVideo != null,
                onClick = {
                    startedAt = System.nanoTime()
                    play(Uri.fromFile(localVideo!!), "datei:")
                },
            ) { Text("Datei") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { results.clear() }) { Text("leeren") }
        }
        if (status.isNotEmpty()) Text(status, color = Color(0xFFBDBDBD), fontSize = 12.sp)
        AndroidView(
            modifier = Modifier.width(160.dp).height(90.dp),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.view_sign_video, android.widget.FrameLayout(ctx), false) as PlayerView
            },
            update = { it.player = player },
        )
        results.take(12).forEach {
            Text(it, color = Color(0xFFE0E0E0), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
