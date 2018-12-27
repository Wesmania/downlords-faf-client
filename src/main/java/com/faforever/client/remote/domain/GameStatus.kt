package com.faforever.client.remote.domain

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.HashMap
import java.util.Locale

enum class GameStatus private constructor(val string: String) {

    UNKNOWN("unknown"),
    PLAYING("playing"),
    OPEN("open"),
    CLOSED("closed");


    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val fromString: MutableMap<String, GameStatus>

        init {
            fromString = HashMap()
            for (gameStatus in values()) {
                fromString[gameStatus.string] = gameStatus
            }
        }

        fun fromString(string: String?): GameStatus {
            val gameStatus = fromString[string?.toLowerCase(Locale.US)]
            if (gameStatus == null) {
                logger.warn("Unknown game state: {}", string)
                return UNKNOWN
            }
            return gameStatus
        }
    }
}
