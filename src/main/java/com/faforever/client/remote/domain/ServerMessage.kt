package com.faforever.client.remote.domain

interface ServerMessage {

    val messageType: ServerMessageType

    val target: MessageTarget
}
