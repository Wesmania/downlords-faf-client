package com.faforever.client.replay

import java.util.concurrent.CompletableFuture

interface ReplayServer {

    fun stop()

    fun start(gameId: Int): CompletableFuture<Int>
}
