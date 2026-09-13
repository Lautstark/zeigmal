package de.lautstark.zeigmal.core

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The four digits in front of the adult mode. Not a secret worth protecting
 * against an adult with the phone in hand — ten thousand combinations, and the
 * private storage is on the same phone; it is the thing that keeps a child
 * who found the corner from finding the writing mode. Stored salted and
 * hashed so a glance at the preferences file does not show it.
 */
class Pin(
    private val store: KeyValueStore,
    private val random: SecureRandom = SecureRandom(),
) {
    val isSet: Boolean get() = store.get(KEY_HASH) != null

    fun set(digits: String) {
        require(digits.matches(Regex("[0-9]{4}"))) { "vier Ziffern" }
        val salt = ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) }
        store.put(KEY_SALT, salt)
        store.put(KEY_HASH, hash(salt, digits))
    }

    fun verify(digits: String): Boolean {
        val salt = store.get(KEY_SALT) ?: return false
        val expected = store.get(KEY_HASH) ?: return false
        return MessageDigest.isEqual(expected.toByteArray(), hash(salt, digits).toByteArray())
    }

    fun clear() {
        store.put(KEY_SALT, null)
        store.put(KEY_HASH, null)
    }

    private fun hash(
        salt: String,
        digits: String,
    ): String = MessageDigest.getInstance("SHA-256").digest("$salt:$digits".toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val LENGTH = 4
        const val KEY_SALT = "pin.salt"
        const val KEY_HASH = "pin.hash"

        /** How long the corner has to be held before the PIN is asked. */
        const val HOLD_MILLIS = 2000L
    }
}
