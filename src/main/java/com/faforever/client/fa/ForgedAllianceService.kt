package com.faforever.client.fa

import com.faforever.client.game.Faction
import com.faforever.client.player.Player
import com.faforever.client.preferences.PreferencesService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.URI
import java.nio.file.Path

import com.faforever.client.preferences.PreferencesService.FORGED_ALLIANCE_EXE

/**
 * Knows how to starts/stop Forged Alliance with proper parameters. Downloading maps, mods and updates as well as
 * notifying the server about whether the preferences is running or not is **not** this service's responsibility.
 */
@Lazy
@Service
class ForgedAllianceService @Inject
constructor(private val preferencesService: PreferencesService) {

    private val executable: Path
        get() = preferencesService.fafBinDirectory.resolve(FORGED_ALLIANCE_EXE)


    @Throws(IOException::class)
    fun startGame(uid: Int, faction: Faction?, additionalArgs: List<String>?,
                  ratingMode: RatingMode, gpgPort: Int, localReplayPort: Int, rehost: Boolean, currentPlayer: Player): Process {
        val executable = executable

        val deviation: Float
        val mean: Float

        when (ratingMode) {
            RatingMode.LADDER_1V1 -> {
                deviation = currentPlayer.leaderboardRatingDeviation
                mean = currentPlayer.leaderboardRatingMean
            }
            else -> {
                deviation = currentPlayer.globalRatingDeviation
                mean = currentPlayer.globalRatingMean
            }
        }

        val launchCommand = defaultLaunchCommand()
                .executable(executable)
                .uid(uid)
                .faction(faction)
                .clan(currentPlayer.clan)
                .country(currentPlayer.country)
                .deviation(deviation)
                .mean(mean)
                .username(currentPlayer.username)
                .additionalArgs(additionalArgs)
                .logFile(preferencesService.fafLogDirectory.resolve("game.log"))
                .localGpgPort(gpgPort)
                .localReplayPort(localReplayPort)
                .rehost(rehost)
                .build()

        return launch(executable, launchCommand)
    }


    @Throws(IOException::class)
    fun startReplay(path: Path, replayId: Int?): Process {
        val executable = executable

        val launchCommand = defaultLaunchCommand()
                .executable(executable)
                .replayFile(path)
                .replayId(replayId)
                .logFile(preferencesService.fafLogDirectory.resolve("game.log"))
                .build()

        return launch(executable, launchCommand)
    }


    @Throws(IOException::class)
    fun startReplay(replayUri: URI, replayId: Int?, currentPlayer: Player): Process {
        val executable = executable

        val launchCommand = defaultLaunchCommand()
                .executable(executable)
                .replayUri(replayUri)
                .replayId(replayId)
                .logFile(preferencesService.fafLogDirectory.resolve("replay.log"))
                .username(currentPlayer.username)
                .build()

        return launch(executable, launchCommand)
    }

    private fun defaultLaunchCommand(): LaunchCommandBuilder {
        return LaunchCommandBuilder.create()
                .executableDecorator(preferencesService.preferences!!.forgedAlliance.executableDecorator)
    }

    @Throws(IOException::class)
    private fun launch(executablePath: Path, launchCommand: List<String>): Process {
        var executeDirectory: Path? = preferencesService.preferences!!.forgedAlliance.executionDirectory
        if (executeDirectory == null) {
            executeDirectory = executablePath.parent
        }

        val processBuilder = ProcessBuilder()
        processBuilder.inheritIO()
        processBuilder.directory(executeDirectory!!.toFile())
        processBuilder.command(launchCommand)

        logger.info("Starting Forged Alliance with command: {} in directory: {}", processBuilder.command(), executeDirectory)

        return processBuilder.start()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
