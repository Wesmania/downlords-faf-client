package com.faforever.client.fa.relay

import com.faforever.client.remote.domain.MessageTarget

import com.faforever.client.fa.relay.GpgServerMessageType.CONNECT_TO_PEER

class ConnectToPeerMessage : GpgServerMessage(CONNECT_TO_PEER, 3) {

    var username: String
        get() = getString(USERNAME_INDEX)
        set(username) = setValue(USERNAME_INDEX, username)

    val peerUid: Int
        get() = getInt(PEER_UID_INDEX)

    val isOffer: Boolean
        get() = getBoolean(OFFER_INDEX)

    init {
        target = MessageTarget.GAME
    }

    companion object {

        private val USERNAME_INDEX = 0
        private val PEER_UID_INDEX = 1
        private val OFFER_INDEX = 2
    }
}
