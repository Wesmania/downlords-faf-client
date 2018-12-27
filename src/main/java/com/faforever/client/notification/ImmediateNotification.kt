package com.faforever.client.notification

/**
 * A notification that requires the user's immediate attention. It is displayed until the user performs a suggested
 * action or dismisses it. The notification consists of a title, a text, an optional image and zero or more actions.
 */
open class ImmediateNotification @JvmOverloads constructor(val title: String, val text: String, val severity: Severity, val throwable: Throwable? = null, val actions: List<Action>? = null) {

    constructor(title: String, text: String, severity: Severity, actions: List<Action>) : this(title, text, severity, null, actions) {}
}
