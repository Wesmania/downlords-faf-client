package com.faforever.client.replay

import com.faforever.client.remote.domain.GameAccess
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.remote.domain.VictoryCondition
import com.faforever.client.remote.gson.GameAccessTypeAdapter
import com.faforever.client.remote.gson.GameStateTypeAdapter
import com.faforever.client.remote.gson.VictoryConditionTypeAdapter
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object ReplayFiles {

    val GSON_BUILDER = GsonBuilder()
            .disableHtmlEscaping()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(GameAccess::class.java, GameAccessTypeAdapter.INSTANCE)
            .registerTypeAdapter(GameStatus::class.java, GameStateTypeAdapter.INSTANCE)
            .registerTypeAdapter(VictoryCondition::class.java, VictoryConditionTypeAdapter.INSTANCE)

    fun gson(): Gson {
        return GSON_BUILDER.create()
    }
}// Utility class
