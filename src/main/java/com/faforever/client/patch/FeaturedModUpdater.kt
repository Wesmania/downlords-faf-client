package com.faforever.client.patch

import com.faforever.client.mod.FeaturedMod

import java.util.concurrent.CompletableFuture

interface FeaturedModUpdater {

    /**
     * Updates the specified featured mod to the specified version. If `version` is null, it will update to the
     * latest version
     */
    fun updateMod(featuredMod: FeaturedMod, version: Int?): CompletableFuture<PatchResult>

    /**
     * Returns `true` if this updater is able to update the specified featured mod.
     */
    fun canUpdate(featuredMod: FeaturedMod): Boolean
}
