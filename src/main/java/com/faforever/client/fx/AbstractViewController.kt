package com.faforever.client.fx

import com.faforever.client.main.event.NavigateEvent
import javafx.scene.Node


abstract class AbstractViewController<ROOT : Node> : Controller<ROOT> {

    fun display(navigateEvent: NavigateEvent) {
        root.isVisible = true
        onDisplay(navigateEvent)
    }

    /**
     * Subclasses may override in order to perform actions when the view is being displayed.
     */
    protected open fun onDisplay(navigateEvent: NavigateEvent) {
        // To be overridden by subclass
    }

    fun hide() {
        root.isVisible = false
        onHide()
    }

    /**
     * Subclasses may override in order to perform actions when the view is no longer being displayed.
     */
    protected open fun onHide() {
        // To be overridden by subclass
    }
}
