package com.faforever.client.remote.domain

interface ServerMessageType {

    val string: String

    fun <T : ServerMessage> getType(): Class<T>
}
