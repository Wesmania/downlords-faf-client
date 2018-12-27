package com.faforever.client.remote.domain

import java.util.HashMap

enum class ServerCommand {
    PING,
    PONG,
    LOGIN_AVAILABLE,
    ACK,
    ERROR,
    MESSAGE;


    companion object {

        private val fromString: MutableMap<String, ServerCommand>

        init {
            fromString = HashMap(values().size, 1f)
            for (serverCommand in values()) {
                fromString[serverCommand.name] = serverCommand
            }
        }

        fun fromString(string: String): ServerCommand {
            return fromString[string]
        }
    }
}
