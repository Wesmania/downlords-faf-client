package com.faforever.client.remote.gson

import com.faforever.client.fa.relay.GpgServerMessageType
import com.faforever.client.remote.domain.FafServerMessageType
import com.faforever.client.remote.domain.MessageTarget
import com.faforever.client.remote.domain.ServerMessage
import com.faforever.client.remote.domain.ServerMessageType
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

import java.lang.reflect.Type

class ServerMessageTypeAdapter : JsonDeserializer<ServerMessage> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ServerMessage? {
        val jsonObject = json.asJsonObject

        val command = jsonObject.get("command").asString
        val targetElement = jsonObject.get("target")

        var target: String? = null
        if (targetElement != null && targetElement !== JsonNull.INSTANCE) {
            target = targetElement.asString
        }

        val messageTarget = MessageTarget.fromString(target)

        val serverMessageType: ServerMessageType?
        when (messageTarget) {
            MessageTarget.GAME, MessageTarget.CONNECTIVITY -> serverMessageType = GpgServerMessageType.fromString(command)

            MessageTarget.CLIENT -> serverMessageType = FafServerMessageType.fromString(command)

            else -> return null
        }

        return if (serverMessageType == null) {
            null
        } else context.deserialize<ServerMessage>(jsonObject, serverMessageType.getType<ServerMessage>())
    }

    companion object {

        val INSTANCE = ServerMessageTypeAdapter()
    }
}
