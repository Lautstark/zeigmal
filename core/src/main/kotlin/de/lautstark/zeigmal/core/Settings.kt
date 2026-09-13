package de.lautstark.zeigmal.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The knobs an adult may turn, kept on the phone. Few on purpose. */
data class SettingsValues(
    /** How often a card that stays in the slot plays before its picture takes over. */
    val maxLoops: Int = Station.MAX_ROUNDS,
)

class Settings(
    private val store: KeyValueStore,
) {
    private val _values =
        MutableStateFlow(
            SettingsValues(
                maxLoops =
                    store.get(KEY_MAX_LOOPS)?.toIntOrNull()?.coerceIn(MIN_LOOPS, MAX_LOOPS) ?: Station.MAX_ROUNDS,
            ),
        )
    val values: StateFlow<SettingsValues> = _values

    fun setMaxLoops(loops: Int) {
        val clamped = loops.coerceIn(MIN_LOOPS, MAX_LOOPS)
        store.put(KEY_MAX_LOOPS, clamped.toString())
        _values.value = _values.value.copy(maxLoops = clamped)
    }

    companion object {
        const val MIN_LOOPS = 1
        const val MAX_LOOPS = 99
        const val KEY_MAX_LOOPS = "loops.max"
    }
}
