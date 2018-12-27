package com.faforever.client.vault.review

import com.faforever.client.remote.FafService
import org.springframework.stereotype.Service

import java.util.concurrent.CompletableFuture

@Service
// TODO remove interfaces from other services as well
class ReviewService(private val fafService: FafService) {

    fun saveGameReview(review: Review, gameId: Int): CompletableFuture<Void> {
        return fafService.saveGameReview(review, gameId)
    }

    fun saveModVersionReview(review: Review, modVersionId: String): CompletableFuture<Void> {
        return fafService.saveModVersionReview(review, modVersionId)
    }

    fun saveMapVersionReview(review: Review, mapVersionId: String): CompletableFuture<Void> {
        return fafService.saveMapVersionReview(review, mapVersionId)
    }

    fun deleteGameReview(review: Review): CompletableFuture<Void> {
        return fafService.deleteGameReview(review)
    }

    fun deleteMapVersionReview(review: Review): CompletableFuture<Void> {
        return fafService.deleteMapVersionReview(review)
    }

    fun deleteModVersionReview(review: Review): CompletableFuture<Void> {
        return fafService.deleteModVersionReview(review)
    }
}
