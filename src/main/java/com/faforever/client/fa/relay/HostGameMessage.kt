package com.faforever.client.fa.relay

import com.faforever.client.remote.domain.MessageTarget

class HostGameMessage : GpgServerMessage(GpgServerMessageType.HOST_GAME, 1) {

    val map: String
        get() = getString(MAP_INDEX)

    init {
        target = MessageTarget.GAME
    }

    companion object {

        private val MAP_INDEX = 0
    }
}
