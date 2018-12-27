package com.faforever.client.fa.relay

import com.faforever.client.remote.JsonMessageSerializer
import com.faforever.client.remote.domain.MessageTarget
import com.faforever.client.remote.gson.GpgClientMessageTypeAdapter
import com.faforever.client.remote.gson.GpgServerMessageTypeTypeAdapter
import com.faforever.client.remote.gson.MessageTargetTypeAdapter
import com.google.gson.GsonBuilder

class GpgClientMessageSerializer : JsonMessageSerializer<GpgGameMessage>() {

    override fun addTypeAdapters(gsonBuilder: GsonBuilder) {
        gsonBuilder.registerTypeAdapter(GpgClientCommand::class.java, GpgClientMessageTypeAdapter.INSTANCE)
        gsonBuilder.registerTypeAdapter(GpgServerMessageType::class.java, GpgServerMessageTypeTypeAdapter.INSTANCE)
        gsonBuilder.registerTypeAdapter(MessageTarget::class.java, MessageTargetTypeAdapter.INSTANCE)
    }
}
