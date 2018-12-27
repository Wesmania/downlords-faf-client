package com.faforever.client.chat

import javafx.scene.Node

import java.util.Optional
import java.util.concurrent.CompletableFuture

interface UrlPreviewResolver {

    fun resolvePreview(urlString: String): CompletableFuture<Optional<Preview>>

    class Preview(val node: Node, val description: String)
}
