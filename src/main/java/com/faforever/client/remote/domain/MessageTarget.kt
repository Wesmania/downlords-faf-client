package com.faforever.client.remote.domain

import java.util.HashMap

enum class MessageTarget private constructor(val string: String) {
    GAME("game"),
    CONNECTIVITY("connectivity"),
    CLIENT(null);


    companion object {

        private val fromString: MutableMap<String, MessageTarget>

        init {
            fromString = HashMap()
            for (messageTarget in values()) {
                fromString[messageTarget.string] = messageTarget
            }
        }

        fun fromString(string: String): MessageTarget {
            return fromString[string]
        }
    }
}
