package com.faforever.client.remote.domain

class PingMessage private constructor() : ClientMessage(ClientMessageType.PING) {
    companion object {

        val INSTANCE = PingMessage()
    }
}
