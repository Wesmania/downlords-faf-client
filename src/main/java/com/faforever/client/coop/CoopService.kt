package com.faforever.client.coop

import com.faforever.client.api.dto.CoopResult
import com.faforever.client.remote.FafService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.concurrent.CompletableFuture


@Lazy
@Service
class CoopService @Inject
constructor(private val fafService: FafService) {

    val missions: CompletableFuture<List<CoopMission>>
        get() = fafService.coopMaps

    fun getLeaderboard(mission: CoopMission, numberOfPlayers: Int): CompletableFuture<List<CoopResult>> {
        return fafService.getCoopLeaderboard(mission, numberOfPlayers)
    }
}
