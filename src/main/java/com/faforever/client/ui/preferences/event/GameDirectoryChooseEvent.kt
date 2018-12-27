package com.faforever.client.ui.preferences.event

import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture

/**
 * Event to be fired whenever the game directory needs to be set.
 */
class GameDirectoryChooseEvent @JvmOverloads constructor(private val future: CompletableFuture<Path>? = null) {

    fun getFuture(): Optional<CompletableFuture<Path>> {
        return Optional.ofNullable(future)
    }
}
