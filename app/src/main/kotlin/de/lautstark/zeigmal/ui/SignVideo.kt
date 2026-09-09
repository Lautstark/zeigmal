package de.lautstark.zeigmal.ui

import android.net.Uri
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import de.lautstark.zeigmal.R
import java.io.File

/**
 * One sign video, and the spoken word beside it when the manifest says so.
 *
 * The order inside the effect is the part that matters, learnt the hard way in
 * knopfpost: build the player without preparing it, attach the listener, then
 * prepare and play. A two-second file off local storage can reach STATE_ENDED
 * before a listener attached afterwards ever hears about it, and a station that
 * misses that stays on the last frame forever.
 */
@Composable
fun SignVideo(
    video: File,
    audio: File?,
    run: Int,
    onEnded: () -> Unit,
) {
    val context = LocalContext.current
    val player =
        remember(video, run) {
            ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.fromFile(video))) }
        }
    val speech =
        remember(audio, run) {
            audio?.let { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(Uri.fromFile(it))) } }
        }
    var ended by remember(player) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) ended = true
                }
            }
        player.addListener(listener)
        player.prepare()
        player.playWhenReady = true
        speech?.prepare()
        speech?.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.release()
            speech?.release()
        }
    }
    LaunchedEffect(ended) { if (ended) onEnded() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.view_sign_video, FrameLayout(ctx), false) as PlayerView },
        update = { view -> view.player = player },
    )
}
