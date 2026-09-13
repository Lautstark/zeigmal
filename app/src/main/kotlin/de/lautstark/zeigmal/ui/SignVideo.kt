package de.lautstark.zeigmal.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import de.lautstark.zeigmal.R

/**
 * One sign video on the shared [player], from a link [resolve] hands back,
 * looping in the player itself — no reload, no gap — until [Station.MAX_ROUNDS]
 * loops have played or the card is gone. Invisible until the first frame has
 * rendered — the ring is what shows until then — and faded in over a few
 * frames.
 *
 * Listener first, then the media item, prepare and play: a short clip can end
 * before a listener attached afterwards hears about it, and a station that
 * misses that stays on the last frame forever.
 */
@Composable
fun SignVideo(
    player: ExoPlayer,
    round: Any,
    resolve: suspend () -> String,
    /** Whether the player should start another loop after the running one; the station decides. */
    loopAgain: Boolean,
    visible: Boolean,
    onFirstFrame: () -> Unit,
    onLooped: () -> Unit,
    onEnded: () -> Unit,
    onFailed: (String) -> Unit,
) {
    var firstFrame by remember(round) { mutableStateOf(false) }
    var ended by remember(round) { mutableStateOf(false) }

    DisposableEffect(round) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    firstFrame = true
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) onLooped()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) ended = true
                }

                override fun onPlayerError(error: PlaybackException) {
                    onFailed(error.errorCodeName)
                }
            }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.stop()
            player.clearMediaItems()
        }
    }
    LaunchedEffect(round) {
        val url =
            try {
                resolve()
            } catch (e: Exception) {
                onFailed(e.message ?: e.javaClass.simpleName)
                return@LaunchedEffect
            }
        player.setMediaItem(MediaItem.fromUri(url.toUri()))
        player.prepare()
        player.playWhenReady = true
    }
    // The station says whether another loop follows; the player only obeys.
    LaunchedEffect(loopAgain) { player.repeatMode = if (loopAgain) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    LaunchedEffect(firstFrame) { if (firstFrame) onFirstFrame() }
    LaunchedEffect(ended) { if (ended) onEnded() }

    val alpha by animateFloatAsState(if (visible && !ended) 1f else 0f, tween(FADE_MILLIS), label = "video")
    AndroidView(
        modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.testTag("video"),
        factory = { ctx -> LayoutInflater.from(ctx).inflate(R.layout.view_sign_video, FrameLayout(ctx), false) as PlayerView },
        update = { view -> view.player = player },
    )
}

private const val FADE_MILLIS = 150
