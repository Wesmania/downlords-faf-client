package com.faforever.client.fa.relay

import com.faforever.client.remote.domain.MessageTarget

class DisconnectFromPeerMessage : GpgServerMessage(GpgServerMessageType.DISCONNECT_FROM_PEER, 1) {

    var uid: Int
        get() = getInt(UID_INDEX)
        set(uid) = setValue(UID_INDEX, uid)

    init {
        target = MessageTarget.GAME
    }

    companion object {

        private val UID_INDEX = 0
    }
}
