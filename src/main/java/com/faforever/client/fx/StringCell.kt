package com.faforever.client.fx

import javafx.geometry.Pos
import javafx.scene.control.TableCell

import java.util.function.Function

class StringCell<S, T>(private val function: Function<T, String>, private val alignment: Pos, vararg cssClasses: String) : TableCell<S, T>() {
    private val cssClasses: Array<String>

    constructor(function: Function<T, String>) : this(function, Pos.CENTER_LEFT) {}

    init {
        this.cssClasses = cssClasses
    }

    override fun updateItem(item: T?, empty: Boolean) {
        super.updateItem(item, empty)

        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = function.apply(item)
            setAlignment(alignment)
            styleClass.addAll(*cssClasses)
        }
    }
}
