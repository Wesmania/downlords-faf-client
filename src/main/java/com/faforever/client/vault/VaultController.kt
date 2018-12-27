package com.faforever.client.vault

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.OpenMapVaultEvent
import com.faforever.client.main.event.OpenModVaultEvent
import com.faforever.client.main.event.OpenOnlineReplayVaultEvent
import com.faforever.client.map.MapVaultController
import com.faforever.client.mod.ModVaultController
import com.faforever.client.replay.OnlineReplayVaultController
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
class VaultController(// TODO change to spring event bus
        private val eventBus: EventBus) : AbstractViewController<Node>() {
    var vaultRoot: TabPane? = null
    var mapVaultTab: Tab? = null
    var modVaultTab: Tab? = null
    var mapVaultController: MapVaultController? = null
    var modVaultController: ModVaultController? = null
    var onlineReplayVaultController: OnlineReplayVaultController? = null
    var onlineReplayVaultTab: Tab? = null
    private var isHandlingEvent: Boolean = false

    override val root: Node?
        get() = vaultRoot

    override fun initialize() {
        vaultRoot!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (isHandlingEvent) {
                return@vaultRoot.getSelectionModel().selectedItemProperty().addListener
            }

            if (newValue === mapVaultTab) {
                eventBus.post(OpenMapVaultEvent())
            } else if (newValue === modVaultTab) {
                eventBus.post(OpenModVaultEvent())
            } else if (newValue === onlineReplayVaultTab) {
                eventBus.post(OpenOnlineReplayVaultEvent())
            }
            // TODO implement other tabs
        }
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        isHandlingEvent = true

        try {
            if (navigateEvent.javaClass == NavigateEvent::class.java || navigateEvent is OpenMapVaultEvent) {
                vaultRoot!!.selectionModel.select(mapVaultTab)
                mapVaultController!!.display(navigateEvent)
            }
            if (navigateEvent is OpenModVaultEvent) {
                vaultRoot!!.selectionModel.select(modVaultTab)
                modVaultController!!.display(navigateEvent)
            }
            if (navigateEvent is OpenOnlineReplayVaultEvent) {
                vaultRoot!!.selectionModel.select(onlineReplayVaultTab)
                onlineReplayVaultController!!.display(navigateEvent)
            }
        } finally {
            isHandlingEvent = false
        }
    }
}
