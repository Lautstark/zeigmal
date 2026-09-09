package de.lautstark.zeigmal.ui

import android.net.Uri
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
 * The view is on screen from the start but invisible until the first frame has
 * actually rendered — the card face underneath is what the child sees until
 * then — and it fades in over a few frames. The spoken word starts with the
 * first frame too, so the word and the sign begin together whatever the disk
 * or decoder needed.
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
    visible: Boolean,
    onFirstFrame: () -> Unit,
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
    var firstFrame by remember(player) { mutableStateOf(false) }
    var ended by remember(player) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    firstFrame = true
                    speech?.playWhenReady = true
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) ended = true
                }
            }
        player.addListener(listener)
        speech?.prepare()
        player.prepare()
        player.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.release()
            speech?.release()
        }
    }
    LaunchedEffect(firstFrame) { if (firstFrame) onFirstFrame() }
    LaunchedEffect(ended) { if (ended) onEnded() }

    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(FADE_MILLIS), label = "video")
    AndroidView(
        modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha },
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.view_sign_video, FrameLayout(ctx), false) as PlayerView },
        update = { view -> view.player = player },
    )
}

private const val FADE_MILLIS = 150
