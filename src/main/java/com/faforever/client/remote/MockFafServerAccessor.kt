package com.faforever.client.remote

import com.faforever.client.FafClientApplication
import com.faforever.client.fa.relay.GpgGameMessage
import com.faforever.client.game.Faction
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.game.NewGameInfo
import com.faforever.client.i18n.I18n
import com.faforever.client.net.ConnectionState
import com.faforever.client.notification.Action
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.Severity
import com.faforever.client.rankedmatch.MatchmakerMessage
import com.faforever.client.rankedmatch.MatchmakerMessage.MatchmakerQueue
import com.faforever.client.remote.domain.Avatar
import com.faforever.client.remote.domain.GameAccess
import com.faforever.client.remote.domain.GameInfoMessage
import com.faforever.client.remote.domain.GameLaunchMessage
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer
import com.faforever.client.remote.domain.LoginMessage
import com.faforever.client.remote.domain.Player
import com.faforever.client.remote.domain.PlayersMessage
import com.faforever.client.remote.domain.RatingRange
import com.faforever.client.remote.domain.ServerMessage
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.TaskService
import com.faforever.client.user.event.LoginSuccessEvent
import com.google.common.eventbus.EventBus
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.net.URL
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import com.faforever.client.remote.domain.GameAccess.PASSWORD
import com.faforever.client.remote.domain.GameAccess.PUBLIC
import com.faforever.client.task.CompletableTask.Priority.HIGH
import java.util.Collections.emptyList
import java.util.Collections.singletonList

@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
// NOSONAR
class MockFafServerAccessor @Inject
constructor(private val taskService: TaskService, private val notificationService: NotificationService, private val i18n: I18n, private val eventBus: EventBus) : FafServerAccessor {
    private val timer: Timer
    private val messageListeners: HashMap<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>

    private val connectionState: ObjectProperty<ConnectionState>

    override val availableAvatars: List<Avatar>
        get() = emptyList()

    override val iceServers: CompletableFuture<List<IceServer>>
        get() = CompletableFuture.completedFuture(emptyList())

    init {
        timer = Timer("LobbyServerAccessorTimer", true)
        messageListeners = HashMap()
        connectionState = SimpleObjectProperty()
    }

    override fun <T : ServerMessage> addOnMessageListener(type: Class<T>, listener: Consumer<T>) {
        if (!messageListeners.containsKey(type)) {
            messageListeners[type] = LinkedList()
        }
        messageListeners[type].add(listener as Consumer<ServerMessage>)
    }

    override fun <T : ServerMessage> removeOnMessageListener(type: Class<T>, listener: Consumer<T>) {
        messageListeners[type].remove(listener)
    }

    override fun connectionStateProperty(): ReadOnlyObjectProperty<ConnectionState> {
        return connectionState
    }

    override fun connectAndLogIn(username: String, password: String): CompletableFuture<LoginMessage> {
        return taskService.submitTask<>(object : CompletableTask<LoginMessage>(HIGH) {
            @Throws(Exception::class)
            override fun call(): LoginMessage {
                updateTitle(i18n.get("login.progress.message"))

                val player = Player()
                player.id = 4812
                player.login = USER_NAME
                player.clan = "ABC"
                player.country = "A1"
                player.globalRating = floatArrayOf(1500f, 220f)
                player.ladderRating = floatArrayOf(1500f, 220f)
                player.numberOfGames = 330

                val playersMessage = PlayersMessage()
                playersMessage.players = listOf(player)

                eventBus.post(LoginSuccessEvent(username, password, player.id))

                (messageListeners as java.util.Map<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>).getOrDefault(playersMessage.javaClass, emptyList()).forEach { consumer -> consumer.accept(playersMessage) }

                timer.schedule(object : TimerTask() {
                    override fun run() {
                        val updatedAchievementsMessage = UpdatedAchievementsMessage()
                        val updatedAchievement = UpdatedAchievement()
                        updatedAchievement.achievementId = "50260d04-90ff-45c8-816b-4ad8d7b97ecd"
                        updatedAchievement.newlyUnlocked = true
                        updatedAchievementsMessage.updatedAchievements = Arrays.asList(updatedAchievement)

                        (messageListeners as java.util.Map<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>).getOrDefault(updatedAchievementsMessage.javaClass, emptyList()).forEach { consumer -> consumer.accept(updatedAchievementsMessage) }
                    }
                }, 7000)

                timer.schedule(object : TimerTask() {
                    override fun run() {
                        val matchmakerServerMessage = MatchmakerMessage()
                        matchmakerServerMessage.queues = listOf(MatchmakerQueue("ladder1v1", listOf(RatingRange(100, 200)), listOf(RatingRange(100, 200))))
                        (messageListeners as java.util.Map<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>).getOrDefault(matchmakerServerMessage.javaClass, emptyList()).forEach { consumer -> consumer.accept(matchmakerServerMessage) }
                    }
                }, 7000)

                val gameInfoMessages = Arrays.asList(
                        createGameInfo(1, "Mock game 500 - 800", PUBLIC, "faf", "scmp_010", 1, 6, "Mock user"),
                        createGameInfo(2, "Mock game 500+", PUBLIC, "faf", "scmp_011", 2, 6, "Mock user"),
                        createGameInfo(3, "Mock game +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"),
                        createGameInfo(4, "Mock game <1000", PUBLIC, "faf", "scmp_013", 4, 6, "Mock user"),
                        createGameInfo(5, "Mock game >1000", PUBLIC, "faf", "scmp_014", 5, 6, "Mock user"),
                        createGameInfo(6, "Mock game ~600", PASSWORD, "faf", "scmp_015", 6, 6, "Mock user"),
                        createGameInfo(7, "Mock game 7", PASSWORD, "faf", "scmp_016", 7, 6, "Mock user")
                )

                gameInfoMessages.forEach { gameInfoMessage ->
                    (messageListeners as java.util.Map<Class<out ServerMessage>, Collection<Consumer<ServerMessage>>>).getOrDefault(gameInfoMessage.javaClass, emptyList())
                            .forEach { consumer -> consumer.accept(gameInfoMessage) }
                }

                notificationService.addNotification(
                        PersistentNotification(
                                "How about a long-running (7s) mock task?",
                                Severity.INFO,
                                Arrays.asList(
                                        Action("Execute") { event ->
                                            taskService.submitTask<>(object : CompletableTask<Void>(HIGH) {
                                                @Throws(Exception::class)
                                                override fun call(): Void? {
                                                    updateTitle("Mock task")
                                                    Thread.sleep(2000)
                                                    for (i in 0..4) {
                                                        updateProgress(i.toLong(), 5)
                                                        Thread.sleep(1000)
                                                    }
                                                    return null
                                                }
                                            })
                                        },
                                        Action("Nope")
                                )
                        )
                )

                val sessionInfo = LoginMessage()
                sessionInfo.id = 123
                sessionInfo.login = USER_NAME
                return sessionInfo
            }
        }).future
    }

    override fun requestHostGame(newGameInfo: NewGameInfo): CompletableFuture<GameLaunchMessage> {
        return taskService.submitTask<>(object : CompletableTask<GameLaunchMessage>(HIGH) {
            @Throws(Exception::class)
            override fun call(): GameLaunchMessage {
                updateTitle("Hosting game")

                val gameLaunchMessage = GameLaunchMessage()
                gameLaunchMessage.args = Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234")
                gameLaunchMessage.mod = "faf"
                gameLaunchMessage.uid = 1234
                return gameLaunchMessage
            }
        }).future
    }

    override fun requestJoinGame(gameId: Int, password: String): CompletableFuture<GameLaunchMessage> {
        return taskService.submitTask<>(object : CompletableTask<GameLaunchMessage>(HIGH) {
            @Throws(Exception::class)
            override fun call(): GameLaunchMessage {
                updateTitle("Joining game")

                val gameLaunchMessage = GameLaunchMessage()
                gameLaunchMessage.args = Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234")
                gameLaunchMessage.mod = "faf"
                gameLaunchMessage.uid = 1234
                return gameLaunchMessage
            }
        }).future
    }

    override fun disconnect() {

    }

    override fun reconnect() {

    }

    override fun addFriend(playerId: Int) {

    }

    override fun addFoe(playerId: Int) {

    }

    override fun startSearchLadder1v1(faction: Faction): CompletableFuture<GameLaunchMessage> {
        logger.debug("Searching 1v1 match with faction: {}", faction)
        val gameLaunchMessage = GameLaunchMessage()
        gameLaunchMessage.uid = 123
        gameLaunchMessage.mod = KnownFeaturedMod.DEFAULT.technicalName
        return CompletableFuture.completedFuture(gameLaunchMessage)
    }

    override fun stopSearchingRanked() {
        logger.debug("Stopping searching 1v1 match")
    }

    override fun sendGpgMessage(message: GpgGameMessage) {

    }

    override fun removeFriend(playerId: Int) {

    }

    override fun removeFoe(playerId: Int) {

    }

    override fun selectAvatar(url: URL) {

    }

    override fun restoreGameSession(id: Int) {

    }

    override fun ping() {

    }


    private fun createGameInfo(uid: Int, title: String, access: GameAccess, featuredMod: String, mapName: String, numPlayers: Int, maxPlayers: Int, host: String): GameInfoMessage {
        val gameInfoMessage = GameInfoMessage()
        gameInfoMessage.setUid(uid)
        gameInfoMessage.setTitle(title)
        gameInfoMessage.setFeaturedMod(featuredMod)
        gameInfoMessage.setMapname(mapName)
        gameInfoMessage.setNumPlayers(numPlayers)
        gameInfoMessage.setMaxPlayers(maxPlayers)
        gameInfoMessage.setHost(host)
        gameInfoMessage.setState(GameStatus.OPEN)
        gameInfoMessage.setSimMods(emptyMap<K, V>())
        gameInfoMessage.setTeams(emptyMap<K, V>())
        gameInfoMessage.setFeaturedModVersions(emptyMap<K, V>())
        gameInfoMessage.setPasswordProtected(access == GameAccess.PASSWORD)

        return gameInfoMessage
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val USER_NAME = "MockUser"
    }
}
