package com.faforever.client.player

import com.faforever.client.chat.ChatChannelUser
import com.faforever.client.chat.ChatUserCreatedEvent
import com.faforever.client.chat.avatar.AvatarBean
import com.faforever.client.chat.avatar.event.AvatarChangedEvent
import com.faforever.client.chat.event.ChatMessageEvent
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.game.Game
import com.faforever.client.game.GameAddedEvent
import com.faforever.client.game.GameRemovedEvent
import com.faforever.client.game.GameUpdatedEvent
import com.faforever.client.player.event.FriendJoinedGameEvent
import com.faforever.client.remote.FafService
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.PlayersMessage
import com.faforever.client.remote.domain.SocialMessage
import com.faforever.client.user.UserService
import com.faforever.client.user.event.LoginSuccessEvent
import com.faforever.client.util.Assert
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.Instant
import java.util.ArrayList
import java.util.HashSet
import java.util.Objects
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

import com.faforever.client.player.SocialStatus.FOE
import com.faforever.client.player.SocialStatus.FRIEND
import com.faforever.client.player.SocialStatus.OTHER
import com.faforever.client.player.SocialStatus.SELF

@Service
class PlayerService(private val fafService: FafService, private val userService: UserService, private val eventBus: EventBus) {

    private val playersByName: ObservableMap<String, Player>
    private val playersById: ObservableMap<Int, Player>
    private val foeList: MutableList<Int>
    private val friendList: MutableList<Int>
    private val currentPlayer: ObjectProperty<Player>

    val playerNames: Set<String>
        get() = HashSet(playersByName.keys)

    init {

        playersByName = FXCollections.observableMap(ConcurrentHashMap())
        playersById = FXCollections.observableHashMap()
        friendList = ArrayList()
        foeList = ArrayList()
        currentPlayer = SimpleObjectProperty()
    }

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
        fafService.addOnMessageListener(PlayersMessage::class.java, { this.onPlayersInfo(it) })
        fafService.addOnMessageListener(SocialMessage::class.java, { this.onFoeList(it) })
    }

    @Subscribe
    fun onGameAdded(event: GameAddedEvent) {
        updateGameForPlayersInGame(event.getGame())
    }

    @Subscribe
    fun onGameUpdated(event: GameUpdatedEvent) {
        updateGameForPlayersInGame(event.getGame())
    }

    @Subscribe
    fun onGameRemoved(event: GameRemovedEvent) {
        val game = event.getGame()
        val teams = game.getTeams()
        synchronized(teams) {
            teams.forEach({ team, players -> updateGamePlayers(players, null) })
        }
    }

    private fun updateGameForPlayersInGame(game: Game) {
        val teams = game.teams
        synchronized(teams) {
            teams.forEach { team, players -> updateGamePlayers(players, game) }
        }
    }

    @Subscribe
    fun onLoginSuccess(event: LoginSuccessEvent) {
        val player = createAndGetPlayerForUsername(event.username)
        player.id = event.userId
        currentPlayer.set(player)
        player.idleSince = Instant.now()
    }

    @Subscribe
    fun onAvatarChanged(event: AvatarChangedEvent) {
        val player = getCurrentPlayer().orElseThrow { IllegalStateException("Player has not been set") }

        val avatar = event.avatar
        if (avatar == null) {
            player.avatarTooltip = null
            player.avatarUrl = null
        } else {
            player.avatarTooltip = avatar.description
            player.avatarUrl = Objects.toString(avatar.url, null)
        }
    }

    @Subscribe
    fun onChatMessage(event: ChatMessageEvent) {
        getPlayerForUsername(event.message.getUsername()).ifPresent(Consumer<Player> { this.resetIdleTime(it) })
    }

    private fun resetIdleTime(playerForUsername: Player) {
        Optional.ofNullable(playerForUsername).ifPresent { player -> player.idleSince = Instant.now() }
    }

    private fun updateGamePlayers(players: List<String>, game: Game?) {
        players.stream()
                .map<Optional<Player>>(Function<String, Optional<Player>> { this.getPlayerForUsername(it) })
                .filter(Predicate<Optional<Player>> { it.isPresent() })
                .map<Player>(Function<Optional<Player>, Player> { it.get() })
                .forEach { player ->
                    resetIdleTime(player)
                    if (game == null || game.status == GameStatus.CLOSED) {
                        player.game = null
                    } else {
                        player.game = game
                        if ((player.game == null || player.game != game) && player.socialStatus == FRIEND && game.status == GameStatus.OPEN) {
                            eventBus.post(FriendJoinedGameEvent(player, game))
                        }
                    }
                }
    }


    fun isOnline(playerId: Int?): Boolean {
        return playersById.containsKey(playerId)
    }

    /**
     * Returns the PlayerInfoBean for the specified username. Returns null if no such player is known.
     */
    fun getPlayerForUsername(username: String?): Optional<Player> {
        return Optional.ofNullable(playersByName[username])
    }

    /**
     * Gets a player for the given username. A new player is created and registered if it does not yet exist.
     */
    internal fun createAndGetPlayerForUsername(username: String): Player {
        Assert.checkNullArgument(username, "username must not be null")

        synchronized(playersByName) {
            if (!playersByName.containsKey(username)) {
                val player = Player(username)
                JavaFxUtil.addListener(player.idProperty()) { observable, oldValue, newValue ->
                    synchronized(playersById) {
                        playersById.remove(oldValue.toInt())
                        playersById.put(newValue.toInt(), player)
                    }
                }
                playersByName[username] = player
            }
        }

        return playersByName[username]
    }

    fun addFriend(player: Player) {
        playersByName[player.username].setSocialStatus(FRIEND)
        friendList.add(player.id)
        foeList.remove(player.id)

        fafService.addFriend(player)
    }

    fun removeFriend(player: Player) {
        playersByName[player.username].setSocialStatus(OTHER)
        friendList.remove(player.id)

        fafService.removeFriend(player)
    }

    fun addFoe(player: Player) {
        playersByName[player.username].setSocialStatus(FOE)
        foeList.add(player.id)
        friendList.remove(player.id)

        fafService.addFoe(player)
    }

    fun removeFoe(player: Player) {
        playersByName[player.username].setSocialStatus(OTHER)
        foeList.remove(player.id)

        fafService.removeFoe(player)
    }

    fun getCurrentPlayer(): Optional<Player> {
        return Optional.ofNullable(currentPlayer.get())
    }

    fun currentPlayerProperty(): ReadOnlyObjectProperty<Player> {
        return currentPlayer
    }

    fun getPlayersByIds(playerIds: Collection<Int>): CompletableFuture<List<Player>> {
        return fafService.getPlayersByIds(playerIds)
    }

    @Subscribe
    fun onChatUserCreated(event: ChatUserCreatedEvent) {
        val chatChannelUser = event.getChatChannelUser()
        Optional.ofNullable(playersByName[chatChannelUser.getUsername()])
                .ifPresent { player ->
                    Platform.runLater {
                        chatChannelUser.setPlayer(player)
                        player.chatChannelUsers.add(chatChannelUser)
                    }
                }
    }

    private fun onPlayersInfo(playersMessage: PlayersMessage) {
        playersMessage.players!!.forEach(Consumer<Player> { this.onPlayerInfo(it) })
    }

    private fun onFoeList(socialMessage: SocialMessage) {
        Optional.ofNullable(socialMessage.foes).ifPresent(Consumer<List<Int>> { this.onFoeList(it) })
        Optional.ofNullable(socialMessage.friends).ifPresent(Consumer<List<Int>> { this.onFriendList(it) })
    }

    private fun onFoeList(foes: List<Int>) {
        updateSocialList(foeList, foes, FOE)
    }

    private fun onFriendList(friends: List<Int>) {
        updateSocialList(friendList, friends, FRIEND)
    }

    private fun updateSocialList(socialList: MutableList<Int>, newValues: List<Int>, socialStatus: SocialStatus) {
        socialList.clear()
        socialList.addAll(newValues)

        synchronized(playersById) {
            for (userId in socialList) {
                val player = playersById[userId]
                if (player != null) {
                    player.socialStatus = socialStatus
                }
            }
        }
    }

    private fun onPlayerInfo(dto: com.faforever.client.remote.domain.Player) {
        if (dto.login!!.equals(userService.username, ignoreCase = true)) {
            val player = getCurrentPlayer().orElseThrow { IllegalStateException("Player has not been set") }
            player.updateFromDto(dto)
            player.socialStatus = SELF
        } else {
            val player = createAndGetPlayerForUsername(dto.login!!)

            if (friendList.contains(dto.id)) {
                player.socialStatus = FRIEND
            } else if (foeList.contains(dto.id)) {
                player.socialStatus = FOE
            } else {
                player.socialStatus = OTHER
            }

            player.updateFromDto(dto)

            eventBus.post(PlayerOnlineEvent(player))
        }
    }
}
