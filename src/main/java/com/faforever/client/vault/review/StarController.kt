package com.faforever.client.vault.review

import com.faforever.client.fx.Controller
import javafx.beans.property.FloatProperty
import javafx.beans.property.ReadOnlyFloatProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Optional
import java.util.function.Consumer

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class StarController : Controller<Pane> {

    var starRoot: StackPane? = null
    var starBackground: Text? = null
    var starFill: Text? = null

    private val fill: FloatProperty
    private var clickedListener: Consumer<StarController>? = null

    override val root: Pane?
        get() = starRoot

    init {
        fill = SimpleFloatProperty(0f)
    }

    override fun initialize() {
        starRoot!!.widthProperty().addListener { observable, oldValue, newValue ->
            val fillClip = Rectangle()
            fillClip.x = 0.0
            fillClip.yProperty().bind(fillClip.heightProperty().negate())
            fillClip.widthProperty().bind(fill.multiply(starFill!!.layoutBounds.width))
            fillClip.heightProperty().bind(starRoot!!.heightProperty())

            starFill!!.clip = fillClip
        }
    }

    fun onMouseClicked() {
        Optional.ofNullable(clickedListener).ifPresent { clickedListener -> clickedListener.accept(this) }
    }

    fun getFill(): Float {
        return fill.get()
    }

    fun setFill(ratio: Float) {
        fill.set(ratio)
    }

    fun fillProperty(): ReadOnlyFloatProperty {
        return fill
    }

    fun setClickListener(clickListener: Consumer<StarController>) {
        this.clickedListener = clickListener
    }
}
