package com.faforever.client.fa.relay

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles
import java.util.HashMap

enum class GpgClientCommand private constructor(val string: String) {
    DISCONNECTED("Disconnected"),
    CONNECTED("Connected"),
    GAME_STATE("GameState"),
    GAME_OPTION("GameOption"),
    GAME_MODS("GameMods"),
    PLAYER_OPTION("PlayerOption"),
    DISCONNECT_FROM_PEER("DisconnectFromPeer"),
    CHAT("Chat"),
    GAME_RESULT("GameResult"),
    STATS("Stats"),
    CLEAR_SLOT("ClearSlot"),
    AI_OPTION("AIOption"),
    JSON_STATS("JsonStats"),
    REHOST("Rehost"),
    DESYNC("Desync"),
    GAME_FULL("GameFull"),
    ENDED("Ended"),
    ICE_MESSAGE("IceMsg"),
    // Yes, this is the only lower-cased command in the protocol. Because reasons.
    CONNECTED_TO_HOST("connectedToHost");


    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val fromString: MutableMap<String, GpgClientCommand>

        init {
            fromString = HashMap()
            for (action in values()) {
                fromString[action.string] = action
            }
        }

        fun fromString(string: String): GpgClientCommand? {
            val action = fromString[string]
            if (action == null) {
                logger.warn("Unknown lobby action: {}", string)
            }
            return action
        }
    }
}
