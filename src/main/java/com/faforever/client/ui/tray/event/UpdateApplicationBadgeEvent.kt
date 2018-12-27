package com.faforever.client.ui.tray.event

import java.util.Optional

/**
 * An application badge is a small icon that indicates new activity in the application, like unread messages. This event
 * either increments (or decrements, if negative) the badge number by a specified delta or specifies the new value.
 */
class UpdateApplicationBadgeEvent private constructor(private val delta: Int?, private val newValue: Int?) {

    fun getDelta(): Optional<Int> {
        return Optional.ofNullable(delta)
    }

    fun getNewValue(): Optional<Int> {
        return Optional.ofNullable(newValue)
    }

    companion object {

        fun ofDelta(delta: Int): UpdateApplicationBadgeEvent {
            return UpdateApplicationBadgeEvent(delta, null)
        }

        fun ofNewValue(newValue: Int): UpdateApplicationBadgeEvent {
            return UpdateApplicationBadgeEvent(null, newValue)
        }
    }
}
