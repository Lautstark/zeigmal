package de.lautstark.zeigmal.ui

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * How much louder the station plays than the clip was mastered, in millibels
 * — a hundredth of a decibel, which is the unit [LoudnessEnhancer] takes.
 *
 * 800 is 8 dB: roughly twice as loud to the ear, and short of where the A51's
 * single bottom-firing speaker starts to rattle in the holder. The effect is
 * a compressor, not a volume knob: it lifts the quiet parts and leaves the
 * peaks where they are, so a spoken word carries across a room without the
 * video's louder moments distorting.
 *
 * Raise it if the room is loud; past about 1200 this speaker buzzes. The real
 * answer for a noisy room is a small active speaker on the headphone jack,
 * which the holder has an opening for (case/building.md).
 */
const val GAIN_MILLIBELS = 800

/**
 * The station's player, with the loudness effect on its own audio session.
 *
 * The clips are streamed and nothing here can normalise them (ADR 0008), so
 * this is the only gain the app has. It hangs off an audio session the app
 * generates itself: attaching to session 0 would boost every sound on the
 * phone, which is not ours to do.
 *
 * The effect is a nice-to-have and never a reason to fail. A device that
 * refuses it — no effect engine, a busy session, a manufacturer that dropped
 * it — plays on at its own volume, because a quiet station beats one that
 * dies over loudness.
 */
@Composable
fun rememberLoudPlayer(gainMilliBels: Int = GAIN_MILLIBELS): ExoPlayer {
    val context = LocalContext.current
    val loud = remember(gainMilliBels) { loudPlayer(context, gainMilliBels) }
    DisposableEffect(loud) {
        onDispose {
            loud.enhancer?.release()
            loud.player.release()
        }
    }
    return loud.player
}

/** A player and the effect hanging off it; the effect is absent when the device refused it. */
private class Loud(
    val player: ExoPlayer,
    val enhancer: LoudnessEnhancer?,
)

// ExoPlayer.setAudioSessionId is media3's own unstable surface. Opted in here
// and nowhere else: the alternative is a project-wide opt-in in lint.xml, and
// a rule that loud everywhere is a rule that stops being read.
@androidx.annotation.OptIn(UnstableApi::class)
private fun loudPlayer(
    context: Context,
    gainMilliBels: Int,
): Loud {
    val session = generateAudioSession(context)
    val player =
        ExoPlayer.Builder(context).build().apply {
            if (session != null) audioSessionId = session
        }
    return Loud(player, session?.let { enhancerOrNull(it, gainMilliBels) })
}

/**
 * An audio session of our own, or null if the system will not give one out.
 * [AudioManager.generateAudioSessionId] answers [AudioManager.ERROR] then, and
 * that value must never reach [LoudnessEnhancer]: 0 is the global output mix.
 */
private fun generateAudioSession(context: Context): Int? {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
    val session = runCatching { audio.generateAudioSessionId() }.getOrNull() ?: return null
    return session.takeIf { it != AudioManager.ERROR && it != 0 }
}

private fun enhancerOrNull(
    session: Int,
    gainMilliBels: Int,
): LoudnessEnhancer? =
    runCatching {
        LoudnessEnhancer(session).apply {
            setTargetGain(gainMilliBels)
            enabled = true
        }
    }.getOrNull()
