package com.faforever.client.fa.relay

import com.faforever.client.remote.domain.MessageTarget

class JoinGameMessage : GpgServerMessage(GpgServerMessageType.JOIN_GAME, 2), Cloneable {

    var username: String
        get() = getString(USERNAME_INDEX)
        set(username) = setValue(USERNAME_INDEX, username)

    val peerUid: Int
        get() = getInt(PEER_UID_INDEX)

    init {
        target = MessageTarget.GAME
    }

    companion object {

        private val USERNAME_INDEX = 0
        private val PEER_UID_INDEX = 1
    }
}
