package com.faforever.client.preferences

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap

class WindowPrefs {

    private val width: IntegerProperty
    private val height: IntegerProperty
    private val maximized: BooleanProperty
    private val lastView: StringProperty
    private val lastChildViews: MapProperty<String, String>
    private val x: DoubleProperty
    private val y: DoubleProperty

    init {
        this.width = SimpleIntegerProperty(800)
        this.height = SimpleIntegerProperty(600)
        x = SimpleDoubleProperty(-1.0)
        y = SimpleDoubleProperty(-1.0)
        maximized = SimpleBooleanProperty()
        lastView = SimpleStringProperty()
        lastChildViews = SimpleMapProperty(FXCollections.observableHashMap())
    }

    fun getWidth(): Int {
        return width.get()
    }

    fun setWidth(width: Int) {
        this.width.set(width)
    }

    fun widthProperty(): IntegerProperty {
        return width
    }

    fun getHeight(): Int {
        return height.get()
    }

    fun setHeight(height: Int) {
        this.height.set(height)
    }

    fun heightProperty(): IntegerProperty {
        return height
    }

    fun getMaximized(): Boolean {
        return maximized.get()
    }

    fun setMaximized(maximized: Boolean) {
        this.maximized.set(maximized)
    }

    fun maximizedProperty(): BooleanProperty {
        return maximized
    }

    fun getLastView(): String {
        return lastView.get()
    }

    fun setLastView(lastView: String) {
        this.lastView.set(lastView)
    }

    fun lastViewProperty(): StringProperty {
        return lastView
    }

    fun getX(): Double {
        return x.get()
    }

    fun setX(x: Double) {
        this.x.set(x)
    }

    fun xProperty(): DoubleProperty {
        return x
    }

    fun getY(): Double {
        return y.get()
    }

    fun setY(y: Double) {
        this.y.set(y)
    }

    fun yProperty(): DoubleProperty {
        return y
    }

    fun getLastChildViews(): ObservableMap<String, String> {
        return lastChildViews.get()
    }

    fun setLastChildViews(lastChildViews: ObservableMap<String, String>) {
        this.lastChildViews.set(lastChildViews)
    }

    fun lastChildViewsProperty(): MapProperty<String, String> {
        return lastChildViews
    }
}
