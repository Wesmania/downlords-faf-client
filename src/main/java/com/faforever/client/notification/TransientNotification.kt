package com.faforever.client.notification

import com.faforever.client.notification.Action.ActionCallback
import javafx.scene.image.Image

import java.util.Objects

/**
 * A notification that is displayed for a short amount of time, or until the user the user performs a suggested actionCallback
 * or dismisses it. The notification consist of a text, an optional image, an optional actionCallback and is always
 * rendered with a close button. The actionCallback is executed when the user clicks on the notification.
 */
class TransientNotification @JvmOverloads constructor(val title: String, val text: String, val image: Image? = null, val actionCallback: ActionCallback? = null) : Comparable<TransientNotification> {

    override fun compareTo(o: TransientNotification): Int {
        return text.compareTo(o.text)
    }

    override fun hashCode(): Int {
        return Objects.hash(title, text, image, actionCallback)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as TransientNotification?
        return title == that!!.title &&
                text == that.text &&
                image == that.image &&
                actionCallback == that.actionCallback
    }
}
