package com.faforever.client.remote.domain

import com.faforever.client.game.GameVisibility
import lombok.Getter
import lombok.Setter

/**
 * Data sent from the client to the FAF server to tell it about a preferences to be hosted.
 */
@Getter
@Setter
class HostGameMessage(private val access: GameAccess, private val mapname: String, private val title: String, private val options: BooleanArray, private val mod: String, private val password: String, private val version: Int?, private val visibility: GameVisibility) : ClientMessage(ClientMessageType.HOST_GAME)
