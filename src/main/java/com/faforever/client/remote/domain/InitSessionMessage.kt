package com.faforever.client.remote.domain

class InitSessionMessage(var version: String?) : ClientMessage(ClientMessageType.ASK_SESSION) {
    private val userAgent = "downlords-faf-client"
}
