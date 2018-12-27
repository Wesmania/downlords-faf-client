package com.faforever.client.ui.preferences

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.ui.StageHolder
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.stage.DirectoryChooser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.Optional

import javafx.application.Platform.runLater


@Component
class GameDirectoryRequiredHandler @Inject
constructor(private val eventBus: EventBus, private val i18n: I18n, preferencesService: PreferencesService) {

    @PostConstruct
    fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onChooseGameDirectory(event: GameDirectoryChooseEvent) {
        runLater {
            val directoryChooser = DirectoryChooser()
            directoryChooser.title = i18n.get("missingGamePath.chooserTitle")
            val result = directoryChooser.showDialog(StageHolder.stage.scene.window)

            logger.info("User selected game directory: {}", result)

            eventBus.post(GameDirectoryChosenEvent(Optional.ofNullable(result).map<Path>(Function<File, Path> { it.toPath() }).orElse(null)))
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
