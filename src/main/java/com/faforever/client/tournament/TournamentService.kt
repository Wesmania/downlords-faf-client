package com.faforever.client.tournament

import com.faforever.client.remote.FafService
import lombok.extern.slf4j.Slf4j
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Lazy
@Service
@Slf4j
class TournamentService(private val fafService: FafService) {

    val allTournaments: CompletableFuture<List<TournamentBean>>
        get() = fafService.allTournaments
}
