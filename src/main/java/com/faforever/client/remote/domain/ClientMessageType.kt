package com.faforever.client.remote.domain

import java.util.HashMap

enum class ClientMessageType private constructor(val string: String) {
    HOST_GAME("game_host"),
    LIST_REPLAYS("list"),
    JOIN_GAME("game_join"),
    ASK_SESSION("ask_session"),
    SOCIAL_ADD("social_add"),
    SOCIAL_REMOVE("social_remove"),
    STATISTICS("stats"),
    LOGIN("hello"),
    GAME_MATCH_MAKING("game_matchmaking"),
    AVATAR("avatar"),
    ICE_SERVERS("ice_servers"),
    RESTORE_GAME_SESSION("restore_game_session"),
    PING("ping"),
    PONG("pong");


    companion object {

        private var fromString: MutableMap<String, ClientMessageType>? = null

        init {
            fromString = HashMap()
            for (clientMessageType in values()) {
                fromString!![clientMessageType.string] = clientMessageType
            }
        }

        fun fromString(string: String): ClientMessageType {
            return fromString!![string]
        }
    }
}
