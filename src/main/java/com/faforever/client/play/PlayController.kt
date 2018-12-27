package com.faforever.client.play

import com.faforever.client.coop.CoopController
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.game.CustomGamesController
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.Open1v1Event
import com.faforever.client.main.event.OpenCoopEvent
import com.faforever.client.main.event.OpenCustomGamesEvent
import com.faforever.client.rankedmatch.Ladder1v1Controller
import com.google.common.eventbus.EventBus
import javafx.scene.Node
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Objects

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PlayController(private val eventBus: EventBus) : AbstractViewController<Node>() {
    override var root: Node? = null
    var customGamesTab: Tab? = null
    var coopTab: Tab? = null
    var ladderTab: Tab? = null
    var playRootTabPane: TabPane? = null
    var customGamesController: CustomGamesController? = null
    var ladderController: Ladder1v1Controller? = null
    var coopController: CoopController? = null
    private var isHandlingEvent: Boolean = false

    override fun initialize() {
        playRootTabPane!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (isHandlingEvent) {
                return@playRootTabPane.getSelectionModel().selectedItemProperty().addListener
            }

            if (newValue === customGamesTab) {
                eventBus.post(OpenCustomGamesEvent())
            } else if (newValue === ladderTab) {
                eventBus.post(Open1v1Event())
            } else if (newValue === coopTab) {
                eventBus.post(OpenCoopEvent())
            }
        }
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        isHandlingEvent = true

        try {
            if (navigateEvent.javaClass == NavigateEvent::class.java || navigateEvent is OpenCustomGamesEvent) {
                playRootTabPane!!.selectionModel.select(customGamesTab)
                customGamesController!!.display(navigateEvent)
            }
            if (navigateEvent is Open1v1Event) {
                playRootTabPane!!.selectionModel.select(ladderTab)
                ladderController!!.display(navigateEvent)
            }
            if (navigateEvent is OpenCoopEvent) {
                playRootTabPane!!.selectionModel.select(coopTab)
                coopController!!.display(navigateEvent)
            }
        } finally {
            isHandlingEvent = false
        }
    }

    override fun onHide() {
        customGamesController!!.hide()
        ladderController!!.hide()
        coopController!!.hide()
    }

}
