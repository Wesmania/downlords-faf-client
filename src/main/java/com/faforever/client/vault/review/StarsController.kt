package com.faforever.client.vault.review

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import javafx.beans.property.FloatProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.css.PseudoClass
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Arrays

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class StarsController : Controller<Pane> {
    private val value: FloatProperty
    var star1Controller: StarController? = null
    var star2Controller: StarController? = null
    var star3Controller: StarController? = null
    var star4Controller: StarController? = null
    var star5Controller: StarController? = null
    override var root: Pane? = null
    private var starControllers: List<StarController>? = null

    init {
        value = SimpleFloatProperty()
    }

    override fun initialize() {
        starControllers = Arrays.asList<StarController>(star1Controller, star2Controller, star3Controller, star4Controller, star5Controller)
        value.addListener { observable, oldValue, newValue ->
            val value = newValue.toInt()
            star1Controller!!.fill = (if (value > 0) 1 else 0).toFloat()
            star2Controller!!.fill = (if (value > 1) 1 else 0).toFloat()
            star3Controller!!.fill = (if (value > 2) 1 else 0).toFloat()
            star4Controller!!.fill = (if (value > 3) 1 else 0).toFloat()
            star5Controller!!.fill = (if (value > 4) 1 else 0).toFloat()
        }
    }

    private fun onStarSelected(starController: StarController) {
        JavaFxUtil.assertApplicationThread()
        val maxActiveStarIndex = starControllers!!.indexOf(starController)
        for (starIndex in starControllers!!.indices) {
            starControllers!![starIndex].fill = (if (starIndex <= maxActiveStarIndex) 1 else 0).toFloat()
        }
        value.set(countActivatedStars().toFloat())
    }

    private fun countActivatedStars(): Int {
        return starControllers!!.stream().filter { starController -> starController.fill >= 1f }.count().toInt()
    }

    fun getValue(): Float {
        return value.get()
    }

    fun setValue(value: Float) {
        this.value.set(value)
    }

    fun valueProperty(): FloatProperty {
        return value
    }

    fun setSelectable(selectable: Boolean) {
        root!!.pseudoClassStateChanged(SELECTABLE_PSEUDO_CLASS, selectable)
        if (selectable) {
            starControllers!!.forEach { starController -> starController.setClickListener(Consumer<StarController> { this.onStarSelected(it) }) }
        } else {
            starControllers!!.forEach { starController -> starController.setClickListener(null) }
        }
    }

    companion object {
        private val SELECTABLE_PSEUDO_CLASS = PseudoClass.getPseudoClass("selectable")
    }
}
