package com.faforever.client.os

import com.faforever.client.replay.ReplayService
import com.install4j.api.launcher.StartupNotification
import lombok.extern.slf4j.Slf4j
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import java.nio.file.Path
import java.nio.file.Paths

/**
 * When a file type is associated with the client and the user opens such a file, this class will handle the opening
 * event. Since Install4j only allows a single listener, all file types need to be handled in this class.
 */
@Component
@Slf4j
class FileOpeningHandler(private val replayService: ReplayService) : ApplicationRunner {

    @PostConstruct
    fun postConstruct() {
        log.debug("Registering file opening handler: {}", this.javaClass.name)
        StartupNotification.registerStartupListener(Listener { this.onStartup(it) })
    }

    private fun onStartup(parameters: String) {
        log.debug("Handling startup: {}", parameters)
        if (parameters.split("\" \"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 2) {
            throw IllegalArgumentException("Can't handle multiple files: $parameters")
        }
        val filePath = Paths.get(parameters.replace("\"", ""))
        runReplay(filePath)
    }

    private fun runReplay(filePath: Path) {
        replayService.runReplayFile(filePath)
    }

    override fun run(args: ApplicationArguments) {
        val sourceArgs = args.sourceArgs
        if (sourceArgs.size > 0) {
            runReplay(Paths.get(sourceArgs[0]))
        }
    }
}
