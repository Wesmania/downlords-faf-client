package com.faforever.client.fx

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.TableCell

import java.util.function.Function

class NodeTableCell<S, T> @JvmOverloads constructor(private val function: Function<T, out Node>, vararg cssClasses: String = arrayOfNulls(0)) : TableCell<S, T>() {
    private val cssClasses: Array<String>

    init {
        this.cssClasses = cssClasses
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)

        Platform.runLater {
            if (empty || item == null) {
                text = null
                graphic = null
            } else {
                graphic = function.apply(item)
                styleClass.addAll(*cssClasses)
            }
        }
    }
}
