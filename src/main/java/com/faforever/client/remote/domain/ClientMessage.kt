package com.faforever.client.remote.domain

import java.util.Collections

open class ClientMessage protected constructor(command: ClientMessageType) : SerializableMessage {

    var command: ClientMessageType? = null
        protected set
    var target: MessageTarget? = null
        protected set

    override val stringsToMask: Collection<String>
        get() = emptyList()

    init {
        this.command = command
    }
}
