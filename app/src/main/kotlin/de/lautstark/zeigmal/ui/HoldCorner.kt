package de.lautstark.zeigmal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.lautstark.zeigmal.core.Pin
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The way into the adult mode: a faint dot in the bottom-right corner, held
 * for [Pin.HOLD_MILLIS]. A hand that brushes it does nothing; a finger that
 * stays gets the PIN screen.
 *
 * Hand-rolled rather than combinedClickable's long press, whose duration comes
 * from the platform's view configuration and could not be made two seconds
 * reliably (knopfpost burned two instrumented runs on that). The semantics
 * node gives a screen reader the same door by name.
 */
@Composable
fun HoldCorner(onHeld: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Box(
            Modifier
                .padding(12.dp)
                .size(56.dp)
                .semantics {
                    onLongClick(label = "Erwachsene") {
                        onHeld()
                        true
                    }
                }.testTag("corner")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val up = withTimeoutOrNull(Pin.HOLD_MILLIS) { waitForUpOrCancellation() }
                        if (up == null) onHeld()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2A2D2C)))
        }
    }
}
