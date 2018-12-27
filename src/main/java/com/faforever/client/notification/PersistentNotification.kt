package com.faforever.client.notification

/**
 * A notification that keeps displaying until the user performs a suggested action or dismisses it. The notification
 * consist of a severity, a text and zero or more actions and is always rendered with a close button.
 */
class PersistentNotification @JvmOverloads constructor(val text: String, val severity: Severity, val actions: List<Action>? = null) : Comparable<PersistentNotification> {

    override fun compareTo(o: PersistentNotification): Int {
        return text.compareTo(o.text)
    }
}
