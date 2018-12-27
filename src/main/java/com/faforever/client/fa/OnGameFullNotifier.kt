package com.faforever.client.fa

import com.faforever.client.config.ClientProperties
import com.faforever.client.fa.relay.event.GameFullEvent
import com.faforever.client.fx.PlatformService
import com.faforever.client.game.Game
import com.faforever.client.game.GameService
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.TransientNotification
import com.faforever.client.util.ProgrammingError
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.util.concurrent.Executor

import com.github.nocatch.NoCatch.noCatch
import java.lang.Thread.sleep

/**
 * Starts flashing the Forged Alliance window whenever a [com.faforever.client.fa.relay.event.GameFullEvent] is
 * triggered and stops as soon as the window is focused. Also shows a transient notification.
 */

@Component
class OnGameFullNotifier @Inject
constructor(private val platformService: PlatformService, private val executor: Executor, private val notificationService: NotificationService,
            private val i18n: I18n, private val mapService: MapService, private val eventBus: EventBus, clientProperties: ClientProperties,
            private val gameService: GameService) {
    private val faWindowTitle: String

    init {
        this.faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle()
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onGameFull(event: GameFullEvent) {
        executor.execute {
            platformService.startFlashingWindow(faWindowTitle)
            while (gameService.isGameRunning && !platformService.isWindowFocused(faWindowTitle)) {
                noCatch { sleep(500) }
            }
            platformService.stopFlashingWindow(faWindowTitle)
        }

        val currentGame = gameService.currentGame
                ?: throw ProgrammingError("Got a GameFull notification but player is not in a game")
        if (platformService.isWindowFocused(faWindowTitle)) {
            return
        }

        notificationService.addNotification(TransientNotification(i18n.get("game.full"), i18n.get("game.full.action"),
                mapService.loadPreview(currentGame.mapFolderName, PreviewSize.SMALL)
        ) { v -> platformService.focusWindow(faWindowTitle) })
    }
}
