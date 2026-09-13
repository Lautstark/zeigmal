package de.lautstark.zeigmal

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import de.lautstark.zeigmal.core.KeyValueStore
import de.lautstark.zeigmal.core.Provider
import de.lautstark.zeigmal.core.SignDigitalProvider
import de.lautstark.zeigmal.core.TagSource
import de.lautstark.zeigmal.nfc.AndroidTagSource

/**
 * The few things the app is made of, in one place, replaceable by a test.
 * Not a framework: three fields with defaults. An instrumented test swaps the
 * tag source for a fake before the activity starts and the whole path from
 * "tag seen" to "first frame" runs on the device without a sticker.
 */
object Deps {
    var store: (Context) -> KeyValueStore = { context -> PreferencesStore(context) }
    var providers: (
        KeyValueStore,
    ) -> Map<String, Provider> = { store -> listOf<Provider>(SignDigitalProvider(store)).associateBy { it.id } }
    var tagSource: (Activity) -> TagSource = { activity -> AndroidTagSource(activity) }

    /** Whether the activity pins itself on start; a test turns it off. */
    var pin: Boolean = true
}

/** The app's private preferences: the login and the writing progress, nothing else. */
class PreferencesStore(
    context: Context,
) : KeyValueStore {
    private val prefs = context.getSharedPreferences("zeigmal", Context.MODE_PRIVATE)

    override fun get(key: String): String? = prefs.getString(key, null)

    override fun put(
        key: String,
        value: String?,
    ) = prefs.edit { if (value == null) remove(key) else putString(key, value) }
}
