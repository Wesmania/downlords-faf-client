package com.faforever.client.replay

import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.PlatformService
import com.faforever.client.game.Game
import com.faforever.client.game.GameService
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.mod.ModService
import com.faforever.client.notification.Action
import com.faforever.client.notification.DismissAction
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.ReportAction
import com.faforever.client.notification.Severity
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.replay.Replay.ChatMessage
import com.faforever.client.replay.Replay.GameOption
import com.faforever.client.reporting.ReportingService
import com.faforever.client.task.TaskService
import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.commons.replay.ReplayData
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Splitter
import com.google.common.net.UrlEscapers
import com.google.common.primitives.Bytes
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

import javax.inject.Inject
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.ArrayList
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import java.util.stream.StreamSupport

import com.faforever.client.notification.Severity.WARN
import com.github.nocatch.NoCatch.noCatch
import java.net.URLDecoder.decode
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files.createDirectories
import java.nio.file.Files.move
import java.util.Arrays.asList
import java.util.Collections.emptyMap
import java.util.Collections.emptySet
import java.util.Collections.singletonList


@Lazy
@Service
@Slf4j
class ReplayService @Inject
constructor(private val clientProperties: ClientProperties, private val preferencesService: PreferencesService,
            private val replayFileReader: ReplayFileReader, private val notificationService: NotificationService,
            private val gameService: GameService, private val taskService: TaskService, private val i18n: I18n,
            private val reportingService: ReportingService, private val applicationContext: ApplicationContext,
            private val platformService: PlatformService, private val replayServer: ReplayServer, private val fafService: FafService,
            private val modService: ModService, private val mapService: MapService) {

    /**
     * Loads some, but not all, local replays. Loading all local replays could result in OOME.
     */
    val localReplays: Collection<Replay>
        @SneakyThrows
        get() {
            val replayInfos = ArrayList<Replay>()

            val replayFileGlob = clientProperties.getReplay().getReplayFileGlob()

            val replaysDirectory = preferencesService.replaysDirectory
            if (Files.notExists(replaysDirectory)) {
                noCatch<Path> { createDirectories(replaysDirectory) }
            }

            Files.newDirectoryStream(replaysDirectory, replayFileGlob).use({ directoryStream ->
                StreamSupport.stream<Path>(directoryStream.spliterator(), false)
                        .limit(MAX_REPLAYS)
                        .forEach { replayFile ->
                            try {
                                val replayInfo = replayFileReader.parseMetaData(replayFile)
                                val featuredMod = modService.getFeaturedMod(replayInfo.featuredMod).getNow(FeaturedMod.UNKNOWN)

                                mapService.findByMapFolderName(replayInfo.mapname)
                                        .thenAccept { mapBean -> replayInfos.add(Replay(replayInfo, replayFile, featuredMod, mapBean.orElse(null))) }
                            } catch (e: Exception) {
                                logger.warn("Could not read replay file '{}'", replayFile, e)
                                moveCorruptedReplayFile(replayFile)
                            }
                        }
            })

            return replayInfos
        }

    private fun moveCorruptedReplayFile(replayFile: Path) {
        val corruptedReplaysDirectory = preferencesService.corruptedReplaysDirectory
        noCatch<Path> { createDirectories(corruptedReplaysDirectory) }

        val target = corruptedReplaysDirectory.resolve(replayFile.fileName)

        logger.debug("Moving corrupted replay file from {} to {}", replayFile, target)

        noCatch<Path> { move(replayFile, target) }

        notificationService.addNotification(PersistentNotification(
                i18n.get("corruptedReplayFiles.notification"), WARN,
                listOf(Action(i18n.get("corruptedReplayFiles.show")) { event -> platformService.reveal(replayFile) })
        ))
    }


    fun runReplay(item: Replay) {
        if (item.replayFile != null) {
            runReplayFile(item.replayFile)
        } else {
            runOnlineReplay(item.id)
        }
    }


    fun runLiveReplay(gameId: Int, playerId: Int) {
        val game = gameService.getByUid(gameId) ?: throw RuntimeException("There's no game with ID: $gameId")

        val uri = UriComponentsBuilder.newInstance()
                .scheme(FAF_LIFE_PROTOCOL)
                .host(clientProperties.getReplay().getRemoteHost())
                .path("/$gameId/$playerId$SUP_COM_REPLAY_FILE_ENDING")
                .queryParam("map", UrlEscapers.urlFragmentEscaper().escape(game.mapFolderName))
                .queryParam("mod", game.featuredMod)
                .build()
                .toUri()

        noCatch { runLiveReplay(uri) }
    }


    fun runLiveReplay(uri: URI) {
        logger.debug("Running replay from URL: {}", uri)
        if (uri.scheme != FAF_LIFE_PROTOCOL) {
            throw IllegalArgumentException("Invalid protocol: " + uri.scheme)
        }

        val queryParams = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(uri.query)

        val gameType = queryParams["mod"]
        val mapName = noCatch<String> { decode(queryParams["map"], UTF_8.name()) }
        val gameId = Integer.parseInt(uri.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])

        try {
            val replayUri = URI(GPGNET_SCHEME, null, uri.host, uri.port, uri.path, null, null)
            gameService.runWithLiveReplay(replayUri, gameId, gameType, mapName)
                    .exceptionally { throwable ->
                        notificationService.addNotification(ImmediateNotification(
                                i18n.get("errorTitle"),
                                i18n.get("liveReplayCouldNotBeStarted"),
                                Severity.ERROR, throwable,
                                asList(DismissAction(i18n), ReportAction(i18n, reportingService, throwable))
                        ))
                        null
                    }
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }

    }


    fun startReplayServer(gameUid: Int): CompletableFuture<Int> {
        return replayServer.start(gameUid)
    }


    fun stopReplayServer() {
        replayServer.stop()
    }


    fun runReplay(replayId: Int?) {
        runOnlineReplay(replayId!!)
    }


    fun getNewestReplays(topElementCount: Int, page: Int): CompletableFuture<List<Replay>> {
        return fafService.getNewestReplays(topElementCount, page)
    }


    fun getHighestRatedReplays(topElementCount: Int, page: Int): CompletableFuture<List<Replay>> {
        return fafService.getHighestRatedReplays(topElementCount, page)
    }


    fun findByQuery(query: String, maxResults: Int, page: Int, sortConfig: SortConfig): CompletableFuture<List<Replay>> {
        return fafService.findReplaysByQuery(query, maxResults, page, sortConfig)
    }


    fun findById(id: Int): CompletableFuture<Optional<Replay>> {
        return fafService.findReplayById(id)

    }


    fun downloadReplay(id: Int): CompletableFuture<Path> {
        val task = applicationContext.getBean(ReplayDownloadTask::class.java)
        task.setReplayId(id)
        return taskService.submitTask(task).future
    }

    /**
     * Reads the specified replay file in order to add more information to the specified replay instance.
     */
    fun enrich(replay: Replay, path: Path) {
        val replayData = replayFileReader.parseReplay(path)
        replay.chatMessages.setAll(replayData.chatMessages.stream()
                .map { chatMessage -> ChatMessage(chatMessage.time, chatMessage.sender, chatMessage.message) }
                .collect<List<ChatMessage>, Any>(Collectors.toList())
        )
        replay.gameOptions.setAll(replayData.gameOptions.stream()
                .map { gameOption -> GameOption(gameOption.key, gameOption.value) }
                .collect<List<GameOption>, Any>(Collectors.toList())
        )
    }


    @SneakyThrows
    fun getSize(id: Int): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            noCatch<Int> {
                URL(String.format(clientProperties.getVault().getReplayDownloadUrlFormat(), id))
                        .openConnection()
                        .contentLength
            }
        }
    }


    fun replayChangedRating(replay: Replay): Boolean {
        return replay.teamPlayerStats.values.stream()
                .flatMap<PlayerStats>(Function<List<PlayerStats>, Stream<out PlayerStats>> { it.stream() })
                .anyMatch { playerStats -> playerStats.getAfterMean() != null && playerStats.getAfterDeviation() != null }
    }

    @SneakyThrows
    fun runReplayFile(path: Path) {
        log.debug("Starting replay file: {}", path.toAbsolutePath())

        val fileName = path.fileName.toString()
        if (fileName.endsWith(FAF_REPLAY_FILE_ENDING)) {
            runFafReplayFile(path)
        } else if (fileName.endsWith(SUP_COM_REPLAY_FILE_ENDING)) {
            runSupComReplayFile(path)
        }
    }

    private fun runOnlineReplay(replayId: Int) {
        downloadReplay(replayId)
                .thenAccept(Consumer<Path> { this.runReplayFile(it) })
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"), i18n.get("replayCouldNotBeStarted", replayId), throwable, i18n, reportingService
                    ))
                    null
                }
    }

    @Throws(IOException::class)
    private fun runFafReplayFile(path: Path) {
        val rawReplayBytes = replayFileReader.readRawReplayData(path)

        val tempSupComReplayFile = preferencesService.cacheDirectory.resolve(TEMP_SCFA_REPLAY_FILE_NAME)

        createDirectories(tempSupComReplayFile.parent)
        Files.copy(ByteArrayInputStream(rawReplayBytes), tempSupComReplayFile, StandardCopyOption.REPLACE_EXISTING)

        val replayInfo = replayFileReader.parseMetaData(path)
        val gameType = replayInfo.featuredMod
        val replayId = replayInfo.uid
        val modVersions = replayInfo.featuredModVersions
        val mapName = replayInfo.mapname

        val simMods = if (replayInfo.simMods != null) replayInfo.simMods!!.keys else emptySet()

        val version = parseSupComVersion(rawReplayBytes)

        gameService.runWithReplay(tempSupComReplayFile, replayId, gameType, version, modVersions, simMods, mapName)
    }

    private fun runSupComReplayFile(path: Path) {
        val rawReplayBytes = replayFileReader.readRawReplayData(path)

        val version = parseSupComVersion(rawReplayBytes)
        val mapName = parseMapName(rawReplayBytes)
        val fileName = path.fileName.toString()
        val gameType = guessModByFileName(fileName)

        gameService.runWithReplay(path, null, gameType, version, emptyMap(), emptySet(), mapName)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        /**
         * Byte offset at which a SupCom replay's version number starts.
         */
        private val VERSION_OFFSET = 0x18
        private val MAP_NAME_OFFSET = 0x2D
        private val FAF_REPLAY_FILE_ENDING = ".fafreplay"
        private val SUP_COM_REPLAY_FILE_ENDING = ".scfareplay"
        private val FAF_LIFE_PROTOCOL = "faflive"
        private val GPGNET_SCHEME = "gpgnet"
        private val TEMP_SCFA_REPLAY_FILE_NAME = "temp.scfareplay"
        private val MAX_REPLAYS: Long = 300

        @VisibleForTesting
        internal fun parseSupComVersion(rawReplayBytes: ByteArray): Int {
            val versionDelimiterIndex = Bytes.indexOf(rawReplayBytes, 0x00.toByte())
            return Integer.parseInt(String(rawReplayBytes, VERSION_OFFSET, versionDelimiterIndex - VERSION_OFFSET, US_ASCII))
        }

        @VisibleForTesting
        internal fun parseMapName(rawReplayBytes: ByteArray): String {
            val mapDelimiterIndex = Bytes.indexOf(rawReplayBytes, byteArrayOf(0x00, 0x0D, 0x0A, 0x1A))
            val mapPath = String(rawReplayBytes, MAP_NAME_OFFSET, mapDelimiterIndex - MAP_NAME_OFFSET, US_ASCII)
            return mapPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2]
        }

        @VisibleForTesting
        internal fun guessModByFileName(fileName: String): String {
            val splitFileName = fileName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return if (splitFileName.size > 2) {
                splitFileName[splitFileName.size - 2]
            } else KnownFeaturedMod.DEFAULT.technicalName
        }
    }
}
