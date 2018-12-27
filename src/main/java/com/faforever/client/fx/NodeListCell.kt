package com.faforever.client.fx

import javafx.scene.Node
import javafx.scene.control.ListCell

import java.util.function.Function

class NodeListCell<T>(private val function: Function<T, out Node>, vararg cssClasses: String) : ListCell<T>() {
    private val cssClasses: Array<String>

    init {
        this.cssClasses = cssClasses
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)

        JavaFxUtil.assertApplicationThread()

        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            graphic = function.apply(item)
            styleClass.addAll(*cssClasses)
        }
    }
}
