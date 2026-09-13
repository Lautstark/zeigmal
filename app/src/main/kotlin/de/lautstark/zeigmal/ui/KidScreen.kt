package de.lautstark.zeigmal.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import de.lautstark.zeigmal.core.CardRecord
import de.lautstark.zeigmal.core.Phase
import de.lautstark.zeigmal.core.StationState

/**
 * The child's screen: black, the mark, and what happens around it. A ring in
 * the accent while a card's video is on its way or between rounds; a grey ring
 * for a sticker with nothing on it; the video in front once its first frame has
 * rendered. No words — the children cannot read yet, and the word is on the
 * card in their hand.
 *
 * One player for the life of the screen; each round swaps the media item.
 */
@Composable
fun KidScreen(
    station: StationState,
    videoUrl: suspend (CardRecord) -> String,
    cardImageUrl: suspend (CardRecord) -> String? = { null },
    onFirstFrame: () -> Unit,
    onEnded: () -> Unit,
    onFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }

    val ring =
        when (station) {
            is StationState.Card -> if (station.phase == Phase.PLAYING) Ring.NONE else Ring.SEEN
            is StationState.Unknown -> Ring.UNKNOWN
            StationState.Idle -> Ring.NONE
        }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (station is StationState.Card && station.phase == Phase.DONE) {
            // The rounds are over: the card in the child's hand, on the screen, until it goes.
            CardPicture(station.record, cardImageUrl)
        } else {
            MarkWithRing(ring)
        }
        if (station is StationState.Card && station.phase != Phase.DONE) {
            SignVideo(
                player = player,
                round = Triple(station.tag, station.record.ref, station.round),
                resolve = { videoUrl(station.record) },
                visible = station.phase == Phase.PLAYING,
                onFirstFrame = onFirstFrame,
                onEnded = onEnded,
                onFailed = onFailed,
            )
        }
    }
}

enum class Ring { NONE, SEEN, UNKNOWN }

@Composable
private fun CardPicture(
    record: CardRecord,
    cardImageUrl: suspend (CardRecord) -> String?,
) {
    val url by produceState<String?>(initialValue = null, record) {
        value = runCatching { cardImageUrl(record) }.getOrNull()
    }
    if (url == null) {
        MarkWithRing(Ring.NONE)
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxHeight(0.9f)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("card-picture"),
        )
    }
}

@Composable
private fun MarkWithRing(ring: Ring) {
    val breath by rememberInfiniteTransition(label = "ring")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "breath")
    val alpha by animateFloatAsState(if (ring == Ring.NONE) 0f else 1f, tween(150), label = "ringAlpha")
    val color = if (ring == Ring.UNKNOWN) Color(0xFF4A4F4D) else Palette.accent
    val breathing = ring == Ring.SEEN
    Box(Modifier.size(220.dp).testTag("ring-${ring.name.lowercase()}"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
            val stroke = (8f + if (breathing) 6f * breath else 0f) * density
            val radius = size.minDimension / 2 - stroke
            drawCircle(color = color, radius = radius, center = Offset(size.width / 2, size.height / 2), style = Stroke(stroke))
        }
        Mark(Modifier.size(120.dp))
    }
}
