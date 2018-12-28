package com.faforever.client.remote.domain

import java.util.Collections

/**
 * Superclass for all server objects. Server objects are deserialized from a JSON-like string, therefore all field names
 * and types must exactly match to what the server sends.. A server object's concrete type is derived by its [ ][.command].
 *
 * @see FafServerMessageType
 */
open class FafServerMessage(
        override val messageType: FafServerMessageType
) : SerializableMessage, ServerMessage {

    /**
     * The server "command" actually isn't a command but identifies the object type.
     */
    override var target: MessageTarget = MessageTarget.CLIENT

    override val stringsToMask: Collection<String>
        get() = emptyList()
}
