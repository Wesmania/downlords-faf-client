package com.faforever.client.fa.relay

import com.faforever.client.remote.domain.IceServerMessage
import com.faforever.client.remote.domain.ServerMessage
import com.faforever.client.remote.domain.ServerMessageType

import java.util.HashMap

/**
 * Enumeration of known server commands (the "command" part of a server domain).
 */
enum class GpgServerMessageType private constructor(override val string: String, private val type: Class<out GpgServerMessage>) : ServerMessageType {
    HOST_GAME("HostGame", HostGameMessage::class.java),
    JOIN_GAME("JoinGame", JoinGameMessage::class.java),
    CONNECT_TO_PEER("ConnectToPeer", ConnectToPeerMessage::class.java),
    ICE_MESSAGE("IceMsg", IceServerMessage::class.java),
    DISCONNECT_FROM_PEER("DisconnectFromPeer", DisconnectFromPeerMessage::class.java);

    override fun <T : ServerMessage> getType(): Class<T> {
        return type as Class<T>
    }

    companion object {


        private val fromString: MutableMap<String, GpgServerMessageType>

        init {
            fromString = HashMap(values().size, 1f)
            for (gpgServerMessageType in values()) {
                fromString[gpgServerMessageType.string] = gpgServerMessageType
            }
        }

        fun fromString(string: String): GpgServerMessageType {
            val gpgServerMessageType = fromString[string]
                    ?: /*
       * If an unknown command is received, ignoring it would probably cause the application to enter an unknown state.
       * So it's better to crash right now so there's no doubt that something went wrong.
       */
                    throw IllegalArgumentException("Unknown relay server command: $string")
            return gpgServerMessageType
        }
    }
}
