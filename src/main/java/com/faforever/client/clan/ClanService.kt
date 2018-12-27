package com.faforever.client.clan

import com.faforever.client.remote.FafService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.Optional
import java.util.concurrent.CompletableFuture

@Lazy
@Service
class ClanService @Inject
constructor(private val fafService: FafService) {

    fun getClanByTag(tag: String): CompletableFuture<Optional<Clan>> {
        return fafService.getClanByTag(tag)
    }
}


