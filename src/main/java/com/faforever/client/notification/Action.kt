package com.faforever.client.notification

import javafx.event.Event

/**
 * Notifications have actions associated with it. This class represents such an action, which is usually displayed to
 * the user as a button.
 */
open class Action
/**
 * Creates an action that calls the specified callback when executed. Also, a type is specified that
 */
@JvmOverloads constructor(val title: String?, val type: Type = Type.OK_DONE, private val callback: ActionCallback? = null) {

    interface ActionCallback {

        fun call(event: Event)
    }

    enum class Type {
        OK_DONE,
        OK_STAY
    }

    constructor(callback: ActionCallback) : this(null, Type.OK_DONE, callback) {}

    /**
     * Creates an action that calls the specified callback when executed. The action will have the default action type
     * [com.faforever.client.notification.Action.Type.OK_DONE].
     */
    constructor(title: String, callback: ActionCallback) : this(title, Type.OK_DONE, callback) {}

    /**
     * Calls the specified callback, if any. Subclasses may override.
     */
    fun call(event: Event) {
        callback?.call(event)
    }
}
/**
 * Creates an action that does nothing.
 */
