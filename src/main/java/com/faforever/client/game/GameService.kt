package com.faforever.client.game

import com.faforever.client.config.ClientProperties
import com.faforever.client.fa.ForgedAllianceService
import com.faforever.client.fa.RatingMode
import com.faforever.client.fa.relay.event.RehostRequestEvent
import com.faforever.client.fa.relay.ice.IceAdapter
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.ShowReplayEvent
import com.faforever.client.map.MapService
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.mod.ModService
import com.faforever.client.net.ConnectionState
import com.faforever.client.notification.Action
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.Severity
import com.faforever.client.patch.GameUpdater
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.NotificationsPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.rankedmatch.MatchmakerMessage
import com.faforever.client.remote.FafService
import com.faforever.client.remote.domain.GameInfoMessage
import com.faforever.client.remote.domain.GameLaunchMessage
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.LoginMessage
import com.faforever.client.replay.ReplayService
import com.faforever.client.reporting.ReportingService
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.URI
import java.nio.file.Path
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Optional
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Consumer

import com.faforever.client.fa.RatingMode.NONE
import com.faforever.client.game.KnownFeaturedMod.LADDER_1V1
import com.github.nocatch.NoCatch.noCatch
import java.util.Collections.emptyMap
import java.util.Collections.emptySet
import java.util.Collections.singletonList
import java.util.concurrent.CompletableFuture.completedFuture

/**
 * Downloads necessary maps, mods and updates before starting
 */
@Lazy
@Service
@Slf4j
class GameService @Inject
constructor(clientProperties: ClientProperties, private val fafService: FafService,
            private val forgedAllianceService: ForgedAllianceService, private val mapService: MapService,
            private val preferencesService: PreferencesService, private val gameUpdater: GameUpdater,
            private val notificationService: NotificationService, private val i18n: I18n, private val executor: Executor,
            private val playerService: PlayerService, private val reportingService: ReportingService, private val eventBus: EventBus,
            private val iceAdapter: IceAdapter, private val modService: ModService, private val platformService: PlatformService) {

    @VisibleForTesting
    internal val gameRunning: BooleanProperty

    /** TODO: Explain why access needs to be synchronized.  */
    @VisibleForTesting
    internal val currentGame: SimpleObjectProperty<Game>

    /**
     * An observable copy of [.uidToGameInfoBean]. **Do not modify its content directly**.
     */
    val games: ObservableList<Game>
    private val uidToGameInfoBean: ObservableMap<Int, Game>
    private val faWindowTitle: String

    //TODO: circular reference
    @Inject
    internal var replayService: ReplayService? = null
    @VisibleForTesting
    internal var ratingMode: RatingMode

    private var process: Process? = null
    private val searching1v1: BooleanProperty
    private var rehostRequested: Boolean = false
    private var localReplayPort: Int = 0

    private val currentPlayer: Player
        get() = playerService.currentPlayer.orElseThrow { IllegalStateException("Player has not been set") }

    private val isRunning: Boolean
        get() = process != null && process!!.isAlive

    var isGameRunning: Boolean
        get() = synchronized(gameRunning) {
            return gameRunning.get()
        }
        private set(running) = synchronized(gameRunning) {
            gameRunning.set(running)
        }

    init {

        faWindowTitle = clientProperties.getForgedAlliance().getWindowTitle()
        uidToGameInfoBean = FXCollections.observableMap(ConcurrentHashMap())
        searching1v1 = SimpleBooleanProperty()
        gameRunning = SimpleBooleanProperty()

        currentGame = SimpleObjectProperty()
        currentGame.addListener { observable, oldValue, newValue ->
            if (newValue == null) {
                return@currentGame.addListener
            }

            val currentGameEndedListener = object : ChangeListener<GameStatus> {
                override fun changed(observable1: ObservableValue<out GameStatus>, oldStatus: GameStatus, newStatus: GameStatus) {
                    if (oldStatus == GameStatus.PLAYING && newStatus == GameStatus.CLOSED) {
                        this@GameService.onCurrentGameEnded()
                    }
                    if (newStatus == GameStatus.CLOSED) {
                        newValue!!.statusProperty().removeListener(this)
                    }
                }
            }
            JavaFxUtil.addListener(newValue!!.statusProperty(), currentGameEndedListener)
        }

        games = FXCollections.observableList(ArrayList()
        ) { item -> arrayOf(item.statusProperty(), item.teams) }
        games.addListener({ change ->
            /* To prevent deadlocks (i.e. synchronization on the game's "teams" and on the google event subscriber), only
      allow this to run on the application thread. */
            JavaFxUtil.assertApplicationThread()

            while (change.next()) {
                change.getRemoved().forEach { game -> eventBus.post(GameRemovedEvent(game)) }

                if (change.wasUpdated()) {
                    for (i in change.getFrom() until change.getTo()) {
                        eventBus.post(GameUpdatedEvent(change.getList().get(i)))
                    }
                }

                change.getAddedSubList().forEach { game -> eventBus.post(GameAddedEvent(game)) }
            }
        } as ListChangeListener<Game>)
        JavaFxUtil.attachListToMap(games, uidToGameInfoBean)
    }

    fun gameRunningProperty(): ReadOnlyBooleanProperty {
        return gameRunning
    }

    fun hostGame(newGameInfo: NewGameInfo): CompletableFuture<Void> {
        if (isRunning) {
            logger.debug("Game is running, ignoring host request")
            return completedFuture(null)
        }

        stopSearchLadder1v1()

        return updateGameIfNecessary(newGameInfo.getFeaturedMod(), null, emptyMap(), newGameInfo.getSimMods())
                .thenCompose { aVoid -> downloadMapIfNecessary(newGameInfo.getMap()) }
                .thenCompose { aVoid -> fafService.requestHostGame(newGameInfo) }
                .thenAccept { gameLaunchMessage -> startGame(gameLaunchMessage, null, RatingMode.GLOBAL) }
    }

    fun joinGame(game: Game, password: String): CompletableFuture<Void> {
        if (isRunning) {
            logger.debug("Game is running, ignoring join request")
            return completedFuture(null)
        }

        logger.info("Joining game: '{}' ({})", game.title, game.id)

        stopSearchLadder1v1()

        val featuredModVersions = game.featuredModVersions
        val simModUIds = game.simMods.keys

        return modService.getFeaturedMod(game.featuredMod)
                .thenCompose { featuredModBean -> updateGameIfNecessary(featuredModBean, null, featuredModVersions, simModUIds) }
                .thenAccept { aVoid ->
                    try {
                        modService.enableSimMods(simModUIds)
                    } catch (e: IOException) {
                        logger.warn("SimMods could not be enabled", e)
                    }
                }
                .thenCompose { aVoid -> downloadMapIfNecessary(game.mapFolderName) }
                .thenCompose { aVoid -> fafService.requestJoinGame(game.id, password) }
                .thenAccept { gameLaunchMessage ->
                    synchronized(currentGame) {
                        // Store password in case we rehost
                        game.password = password
                        currentGame.set(game)
                    }
                    startGame(gameLaunchMessage, null, RatingMode.GLOBAL)
                }
                .exceptionally { throwable ->
                    log.warn("Game could not be joined", throwable)
                    notificationService.addImmediateErrorNotification(throwable, "games.couldNotJoin")
                    null
                }
    }

    private fun downloadMapIfNecessary(mapFolderName: String?): CompletableFuture<Void> {
        return if (mapService.isInstalled(mapFolderName)) {
            completedFuture(null)
        } else mapService.download(mapFolderName)
    }

    /**
     * @param path a replay file that is readable by the preferences without any further conversion
     */

    fun runWithReplay(path: Path, replayId: Int?, featuredMod: String, version: Int?, modVersions: Map<String, Int>, simMods: Set<String>, mapName: String) {
        if (isRunning) {
            logger.warn("Forged Alliance is already running, not starting replay")
            return
        }
        modService.getFeaturedMod(featuredMod)
                .thenCompose { featuredModBean -> updateGameIfNecessary(featuredModBean, version, modVersions, simMods) }
                .thenCompose { aVoid -> downloadMapIfNecessary(mapName) }
                .thenRun {
                    try {
                        process = forgedAllianceService.startReplay(path, replayId)
                        isGameRunning = true
                        this.ratingMode = NONE
                        spawnTerminationListener(process)
                    } catch (e: IOException) {
                        notifyCantPlayReplay(replayId, e)
                    }
                }
                .exceptionally { throwable ->
                    notifyCantPlayReplay(replayId, throwable)
                    null
                }
    }

    private fun notifyCantPlayReplay(replayId: Int?, throwable: Throwable) {
        logger.error("Could not play replay '$replayId'", throwable)
        notificationService.addNotification(ImmediateErrorNotification(
                i18n.get("errorTitle"),
                i18n.get("replayCouldNotBeStarted", replayId),
                throwable,
                i18n, reportingService
        ))
    }

    fun runWithLiveReplay(replayUrl: URI, gameId: Int?, gameType: String, mapName: String): CompletableFuture<Void> {
        if (isRunning) {
            logger.warn("Forged Alliance is already running, not starting live replay")
            return completedFuture(null)
        }

        val gameBean = getByUid(gameId!!)

        val modVersions = gameBean!!.featuredModVersions
        val simModUids = gameBean.simMods.keys

        return modService.getFeaturedMod(gameType)
                .thenCompose { featuredModBean -> updateGameIfNecessary(featuredModBean, null, modVersions, simModUids) }
                .thenCompose { aVoid -> downloadMapIfNecessary(mapName) }
                .thenRun {
                    noCatch {
                        process = forgedAllianceService.startReplay(replayUrl, gameId, currentPlayer)
                        isGameRunning = true
                        this.ratingMode = NONE
                        spawnTerminationListener(process)
                    }
                }
    }


    fun getByUid(uid: Int): Game? {
        val game = uidToGameInfoBean[uid]
        if (game == null) {
            logger.warn("Can't find {} in gameInfoBean map", uid)
        }
        return game
    }

    fun addOnRankedMatchNotificationListener(listener: Consumer<MatchmakerMessage>) {
        fafService.addOnMessageListener(MatchmakerMessage::class.java, listener)
    }

    fun startSearchLadder1v1(faction: Faction): CompletableFuture<Void> {
        if (isRunning) {
            logger.debug("Game is running, ignoring 1v1 search request")
            return completedFuture(null)
        }

        searching1v1.set(true)

        val port = preferencesService.preferences!!.forgedAlliance.port

        return modService.getFeaturedMod(LADDER_1V1.technicalName)
                .thenAccept { featuredModBean -> updateGameIfNecessary(featuredModBean, null, emptyMap(), emptySet()) }
                .thenCompose { aVoid -> fafService.startSearchLadder1v1(faction, port) }
                .thenAccept { gameLaunchMessage ->
                    downloadMapIfNecessary(gameLaunchMessage.mapname)
                            .thenRun {
                                // TODO this should be sent by the server!
                                gameLaunchMessage.args = ArrayList(gameLaunchMessage.args!!)
                                gameLaunchMessage.args!!.add("/team 1")
                                gameLaunchMessage.args!!.add("/players 2")

                                startGame(gameLaunchMessage, faction, RatingMode.LADDER_1V1)
                            }
                }
                .exceptionally { throwable ->
                    if (throwable is CancellationException) {
                        logger.info("Ranked1v1 search has been cancelled")
                    } else {
                        logger.warn("Ranked1v1 could not be started", throwable)
                    }
                    null
                }
    }

    fun stopSearchLadder1v1() {
        if (searching1v1.get()) {
            fafService.stopSearchingRanked()
            searching1v1.set(false)
        }
    }

    fun searching1v1Property(): BooleanProperty {
        return searching1v1
    }

    /**
     * Returns the preferences the player is currently in. Returns `null` if not in a preferences.
     */
    fun getCurrentGame(): Game? {
        synchronized(currentGame) {
            return currentGame.get()
        }
    }

    private fun updateGameIfNecessary(featuredMod: FeaturedMod, version: Int?, featuredModVersions: Map<String, Int>, simModUids: Set<String>): CompletableFuture<Void> {
        return gameUpdater.update(featuredMod, version, featuredModVersions, simModUids)
    }

    /**
     * Actually starts the game, including relay and replay server. Call this method when everything else is prepared
     * (mod/map download, connectivity check etc.)
     */
    private fun startGame(gameLaunchMessage: GameLaunchMessage, faction: Faction?, ratingMode: RatingMode) {
        if (isRunning) {
            logger.warn("Forged Alliance is already running, not starting game")
            return
        }

        stopSearchLadder1v1()
        replayService!!.startReplayServer(gameLaunchMessage.uid)
                .thenCompose { port ->
                    localReplayPort = port!!
                    iceAdapter.start()
                }
                .thenAccept { adapterPort ->
                    val args = fixMalformedArgs(gameLaunchMessage.args!!)
                    process = noCatch<Process> {
                        forgedAllianceService.startGame(gameLaunchMessage.uid, faction, args, ratingMode,
                                adapterPort!!, localReplayPort, rehostRequested, currentPlayer)
                    }
                    isGameRunning = true

                    this.ratingMode = ratingMode
                    spawnTerminationListener(process)
                }
                .exceptionally { throwable ->
                    logger.warn("Game could not be started", throwable)
                    notificationService.addNotification(
                            ImmediateErrorNotification(i18n.get("errorTitle"), i18n.get("game.start.couldNotStart"), throwable, i18n, reportingService)
                    )
                    isGameRunning = false
                    null
                }
    }

    private fun onCurrentGameEnded() {
        val notification = preferencesService.preferences!!.notification
        if (!notification.isAfterGameReviewEnabled || !notification.isTransientNotificationsEnabled) {
            return
        }

        synchronized(currentGame) {
            val id = currentGame.get().id
            notificationService.addNotification(PersistentNotification(i18n.get("game.ended", currentGame.get().title),
                    Severity.INFO,
                    listOf(Action(i18n.get("game.rate")) { actionEvent ->
                        replayService!!.findById(id)
                                .thenAccept { replay ->
                                    Platform.runLater {
                                        if (replay.isPresent) {
                                            eventBus.post(ShowReplayEvent(replay.get()))
                                        } else {
                                            notificationService.addNotification(ImmediateNotification(i18n.get("replay.notFoundTitle"), i18n.get("replay.replayNotFoundText", id), Severity.WARN))
                                        }
                                    }
                                }
                    })))
        }
    }

    /**
     * A correct argument list looks like ["/ratingcolor", "d8d8d8d8", "/numgames", "236"]. However, the FAF server sends
     * it as ["/ratingcolor d8d8d8d8", "/numgames 236"]. This method fixes this.
     */
    private fun fixMalformedArgs(gameLaunchMessage: List<String>): List<String> {
        val fixedArgs = ArrayList<String>()

        for (combinedArg in gameLaunchMessage) {
            val split = combinedArg.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            Collections.addAll(fixedArgs, *split)
        }
        return fixedArgs
    }

    @VisibleForTesting
    internal fun spawnTerminationListener(process: Process?) {
        executor.execute {
            try {
                rehostRequested = false
                val exitCode = process!!.waitFor()
                logger.info("Forged Alliance terminated with exit code {}", exitCode)

                synchronized(gameRunning) {
                    gameRunning.set(false)
                    fafService.notifyGameEnded()
                    replayService!!.stopReplayServer()
                    iceAdapter.stop()

                    if (rehostRequested) {
                        rehost()
                    }
                }
            } catch (e: InterruptedException) {
                logger.warn("Error during post-game processing", e)
            }
        }
    }

    private fun rehost() {
        synchronized(currentGame) {
            val game = currentGame.get()

            modService.getFeaturedMod(game.featuredMod)
                    .thenAccept { featuredModBean ->
                        hostGame(NewGameInfo(
                                game.title,
                                game.password,
                                featuredModBean,
                                game.mapFolderName,
                                HashSet(game.simMods.values)
                        ))
                    }
        }
    }

    @Subscribe
    fun onRehostRequest(event: RehostRequestEvent) {
        this.rehostRequested = true
        synchronized(gameRunning) {
            if (!gameRunning.get()) {
                // If the game already has terminated, the rehost is issued here. Otherwise it will be issued after termination
                rehost()
            }
        }
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
        fafService.addOnMessageListener(GameInfoMessage::class.java) { message -> Platform.runLater { onGameInfo(message) } }
        fafService.addOnMessageListener(LoginMessage::class.java) { message -> onLoggedIn() }
        JavaFxUtil.addListener(fafService.connectionStateProperty()) { observable, oldValue, newValue ->
            if (newValue == ConnectionState.DISCONNECTED) {
                synchronized(uidToGameInfoBean) {
                    uidToGameInfoBean.clear()
                }
            }
        }
    }

    private fun onLoggedIn() {
        if (isGameRunning) {
            fafService.restoreGameSession(currentGame.get().id)
        }
    }

    private fun onGameInfo(gameInfoMessage: GameInfoMessage) {
        // Since all game updates are usually reflected on the UI and to prevent deadlocks
        JavaFxUtil.assertApplicationThread()

        if (gameInfoMessage.games != null) {
            gameInfoMessage.games!!.forEach(Consumer { this.onGameInfo(it) })
            return
        }

        // We may receive game info before we receive our player info
        val currentPlayerOptional = playerService.currentPlayer

        val game = createOrUpdateGame(gameInfoMessage)
        if (GameStatus.CLOSED == game.status) {
            if (!currentPlayerOptional.isPresent || currentPlayerOptional.get().game !== game) {
                removeGame(gameInfoMessage)
                return
            }

            // Don't remove the game until the current player closed it. TODO: Why?
            JavaFxUtil.addListener(currentPlayerOptional.get().gameProperty()) { observable, oldValue, newValue ->
                if (newValue == null && oldValue.status == GameStatus.CLOSED) {
                    removeGame(gameInfoMessage)
                }
            }
        }

        if (currentPlayerOptional.isPresent) {
            // TODO the following can be removed as soon as the server tells us which game a player is in.
            val currentPlayerInGame = gameInfoMessage.getTeams().values().stream()
                    .anyMatch({ team -> team.contains(currentPlayerOptional.get().username) })

            if (currentPlayerInGame && GameStatus.OPEN == gameInfoMessage.getState()) {
                synchronized(currentGame) {
                    currentGame.set(game)
                }
            }
        }

        JavaFxUtil.addListener(game.statusProperty()) { observable, oldValue, newValue ->
            if (oldValue == GameStatus.OPEN
                    && newValue == GameStatus.PLAYING
                    && game.teams.values.stream().anyMatch { team -> playerService.currentPlayer.isPresent && team.contains(playerService.currentPlayer.get().username) }
                    && !platformService.isWindowFocused(faWindowTitle)) {
                platformService.focusWindow(faWindowTitle)
            }
        }
    }

    private fun createOrUpdateGame(gameInfoMessage: GameInfoMessage): Game {
        val gameId = gameInfoMessage.getUid()
        val game: Game
        synchronized(uidToGameInfoBean) {
            if (!uidToGameInfoBean.containsKey(gameId)) {
                game = Game(gameInfoMessage)
                uidToGameInfoBean.put(gameId, game)
            } else {
                game = uidToGameInfoBean[gameId]
                game.updateFromGameInfo(gameInfoMessage)
            }
        }
        return game
    }

    private fun removeGame(gameInfoMessage: GameInfoMessage) {
        synchronized(uidToGameInfoBean) {
            uidToGameInfoBean.remove(gameInfoMessage.getUid())
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
