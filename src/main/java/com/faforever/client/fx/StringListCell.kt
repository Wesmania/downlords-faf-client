package com.faforever.client.fx

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ListCell

import java.util.Objects
import java.util.function.Function

class StringListCell<T>(private val function: Function<T, String>, private val graphicFunction: Function<T, Node>?, private val alignment: Pos, vararg cssClasses: String) : ListCell<T>() {
    private val cssClasses: Array<String>

    @JvmOverloads
    constructor(function: Function<T, String>, graphicFunction: Function<T, Node>? = null) : this(function, graphicFunction, Pos.CENTER_LEFT) {
    }

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
                if (graphicFunction != null) {
                    graphic = graphicFunction.apply(item)
                }
                text = Objects.toString(function.apply(item), "")
                setAlignment(alignment)
                styleClass.addAll(*cssClasses)
            }
        }
    }
}
