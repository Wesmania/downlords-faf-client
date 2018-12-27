package com.faforever.client.remote

import com.faforever.client.game.Faction
import com.faforever.client.game.GameVisibility
import com.faforever.client.remote.domain.ClientMessage
import com.faforever.client.remote.domain.ClientMessageType
import com.faforever.client.remote.domain.GameAccess
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.MessageTarget
import com.faforever.client.remote.domain.VictoryCondition
import com.faforever.client.remote.gson.ClientMessageTypeTypeAdapter
import com.faforever.client.remote.gson.FactionTypeAdapter
import com.faforever.client.remote.gson.GameAccessTypeAdapter
import com.faforever.client.remote.gson.GameStateTypeAdapter
import com.faforever.client.remote.gson.GameVisibilityTypeAdapter
import com.faforever.client.remote.gson.MessageTargetTypeAdapter
import com.faforever.client.remote.gson.VictoryConditionTypeAdapter
import com.google.gson.GsonBuilder

class ClientMessageSerializer : JsonMessageSerializer<ClientMessage>() {

    override fun addTypeAdapters(gsonBuilder: GsonBuilder) {
        gsonBuilder.registerTypeAdapter(GameAccess::class.java, GameAccessTypeAdapter.INSTANCE)
                .registerTypeAdapter(GameStatus::class.java, GameStateTypeAdapter.INSTANCE)
                .registerTypeAdapter(ClientMessageType::class.java, ClientMessageTypeTypeAdapter.INSTANCE)
                .registerTypeAdapter(VictoryCondition::class.java, VictoryConditionTypeAdapter.INSTANCE)
                .registerTypeAdapter(Faction::class.java, FactionTypeAdapter.INSTANCE)
                .registerTypeAdapter(GameVisibility::class.java, GameVisibilityTypeAdapter.INSTANCE)
                .registerTypeAdapter(MessageTarget::class.java, MessageTargetTypeAdapter.INSTANCE)
    }
}
