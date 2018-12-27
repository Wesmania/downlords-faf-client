package com.faforever.client.fa.relay.ice

import java.util.concurrent.CompletableFuture

/**
 * Starts or stops the ICE adapter process.
 */
interface IceAdapter {
    fun start(): CompletableFuture<Int>

    fun stop()
}
