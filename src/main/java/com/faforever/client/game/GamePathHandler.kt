package com.faforever.client.game

import com.faforever.client.i18n.I18n
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.Severity
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.preferences.event.MissingGamePathEvent
import com.faforever.client.ui.preferences.event.GameDirectoryChosenEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.lang.invoke.MethodHandles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Arrays

@Component
class GamePathHandler(private val notificationService: NotificationService, private val i18n: I18n, private val eventBus: EventBus, private val preferencesService: PreferencesService) {

    @PostConstruct
    fun postConstruct() {
        eventBus.register(this)
    }

    /**
     * Checks whether the chosen game path contains a ForgedAlliance.exe (either directly if the user selected the "bin"
     * directory, or in the "bin" sub folder). If the path is valid, it is stored in the preferences.
     */
    @Subscribe
    fun onGameDirectoryChosenEvent(event: GameDirectoryChosenEvent) {
        val path = event.path

        if (path == null || !Files.isDirectory(path)) {
            notificationService.addNotification(ImmediateNotification(i18n.get("gameChosen.invalidPath"), i18n.get("gameChosen.couldNotLocatedGame"), Severity.WARN))
            return
        }

        if (!Files.isRegularFile(path.resolve(PreferencesService.FORGED_ALLIANCE_EXE)) && !Files.isRegularFile(path.resolve(PreferencesService.SUPREME_COMMANDER_EXE))) {
            onGameDirectoryChosenEvent(GameDirectoryChosenEvent(path.resolve("bin")))
            return
        }

        // At this point, path points to the "bin" directory
        val gamePath = path.parent

        logger.info("Found game path at {}", gamePath)
        preferencesService.preferences!!.forgedAlliance.path = gamePath
        preferencesService.storeInBackground()
    }


    private fun detectGamePath() {
        for (path in USUAL_GAME_PATHS) {
            if (preferencesService.isGamePathValid(path.resolve("bin"))) {
                onGameDirectoryChosenEvent(GameDirectoryChosenEvent(path))
                return
            }
        }

        logger.info("Game path could not be detected")
        eventBus.post(MissingGamePathEvent())
    }

    fun detectAndUpdateGamePath() {
        val faPath = preferencesService.preferences!!.forgedAlliance.path
        if (faPath == null || Files.notExists(faPath)) {
            logger.info("Game path is not specified or non-existent, trying to detect")
            detectGamePath()
        }
    }

    companion object {
        private val USUAL_GAME_PATHS = Arrays.asList(
                Paths.get(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
                Paths.get(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
                Paths.get(System.getenv("ProgramFiles") + "\\Steam\\steamapps\\common\\supreme commander forged alliance"),
                Paths.get(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")
        )
        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
