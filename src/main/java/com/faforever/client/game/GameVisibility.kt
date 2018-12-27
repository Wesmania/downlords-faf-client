package com.faforever.client.game

import java.util.HashMap

enum class GameVisibility private constructor(val string: String) {
    PUBLIC("public"),
    PRIVATE("friends");


    companion object {

        private val fromString: HashMap<String, GameVisibility>

        init {
            fromString = HashMap()
            for (gameVisibility in values()) {
                fromString[gameVisibility.string] = gameVisibility
            }
        }

        fun fromString(string: String): GameVisibility {
            return fromString[string]
        }
    }
}
