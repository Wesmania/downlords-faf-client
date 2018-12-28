package com.faforever.client.remote

import com.faforever.client.fa.relay.GpgGameMessage
import com.faforever.client.game.Faction
import com.faforever.client.game.NewGameInfo
import com.faforever.client.net.ConnectionState
import com.faforever.client.remote.domain.Avatar
import com.faforever.client.remote.domain.GameLaunchMessage
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer
import com.faforever.client.remote.domain.LoginMessage
import com.faforever.client.remote.domain.ServerMessage
import javafx.beans.property.ReadOnlyObjectProperty

import java.net.URL
import java.util.concurrent.CompletableFuture

/**
 * Entry class for all communication with the FAF server.
 */
interface FafServerAccessor {

    val availableAvatars: List<Avatar>

    val iceServers: CompletableFuture<List<IceServer>>

    fun <T : ServerMessage> addOnMessageListener(type: Class<T>, listener: (T) -> Unit)

    fun <T : ServerMessage> removeOnMessageListener(type: Class<T>, listener: (T) -> Unit)

    fun connectionStateProperty(): ReadOnlyObjectProperty<ConnectionState>

    fun connectAndLogIn(username: String, password: String): CompletableFuture<LoginMessage>

    fun requestHostGame(newGameInfo: NewGameInfo): CompletableFuture<GameLaunchMessage>

    fun requestJoinGame(gameId: Int, password: String): CompletableFuture<GameLaunchMessage>

    fun disconnect()

    fun reconnect()

    fun addFriend(playerId: Int)

    fun addFoe(playerId: Int)

    fun startSearchLadder1v1(faction: Faction): CompletableFuture<GameLaunchMessage>

    fun stopSearchingRanked()

    fun sendGpgMessage(message: GpgGameMessage)

    fun removeFriend(playerId: Int)

    fun removeFoe(playerId: Int)

    fun selectAvatar(url: URL)

    fun restoreGameSession(id: Int)

    fun ping()
}
