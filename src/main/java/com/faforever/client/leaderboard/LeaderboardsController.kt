package com.faforever.client.leaderboard

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.OpenGlobalLeaderboardEvent
import com.faforever.client.main.event.OpenLadder1v1LeaderboardEvent
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
class LeaderboardsController(private val eventBus: EventBus) : AbstractViewController<Node>() {

    var leaderboardRoot: TabPane? = null
    var ladder1v1LeaderboardController: LeaderboardController? = null
    var globalLeaderboardController: LeaderboardController? = null
    var ladder1v1LeaderboardTab: Tab? = null
    var globalLeaderboardTab: Tab? = null

    private var isHandlingEvent: Boolean = false

    override val root: Node?
        get() = leaderboardRoot

    override fun initialize() {
        ladder1v1LeaderboardController!!.setRatingType(KnownFeaturedMod.LADDER_1V1)
        globalLeaderboardController!!.setRatingType(KnownFeaturedMod.FAF)

        leaderboardRoot!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (isHandlingEvent) {
                return@leaderboardRoot.getSelectionModel().selectedItemProperty().addListener
            }

            if (newValue === ladder1v1LeaderboardTab) {
                eventBus.post(OpenLadder1v1LeaderboardEvent())
            } else if (newValue === globalLeaderboardTab) {
                eventBus.post(OpenGlobalLeaderboardEvent())
            }
            // TODO implement other tabs
        }
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        isHandlingEvent = true

        try {
            if (navigateEvent.javaClass == NavigateEvent::class.java || navigateEvent is OpenLadder1v1LeaderboardEvent) {
                leaderboardRoot!!.selectionModel.select(ladder1v1LeaderboardTab)
                ladder1v1LeaderboardController!!.display(navigateEvent)
            }
            if (navigateEvent is OpenGlobalLeaderboardEvent) {
                leaderboardRoot!!.selectionModel.select(globalLeaderboardTab)
                globalLeaderboardController!!.display(navigateEvent)
            }
        } finally {
            isHandlingEvent = false
        }
    }
}
