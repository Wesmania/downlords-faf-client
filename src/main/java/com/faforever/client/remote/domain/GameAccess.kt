package com.faforever.client.remote.domain

import java.util.HashMap

enum class GameAccess private constructor(val string: String) {
    PUBLIC("public"),
    PASSWORD("password");


    companion object {

        private val fromString: MutableMap<String, GameAccess>

        init {
            fromString = HashMap(values().size, 1f)
            for (gameAccess in values()) {
                fromString[gameAccess.string] = gameAccess
            }
        }

        fun fromString(string: String): GameAccess {
            return fromString[string]
        }
    }
}
