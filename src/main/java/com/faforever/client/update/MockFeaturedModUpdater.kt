package com.faforever.client.update

import com.faforever.client.FafClientApplication
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.patch.FeaturedModUpdater
import com.faforever.client.patch.PatchResult
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

import java.util.concurrent.CompletableFuture


@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
class MockFeaturedModUpdater : FeaturedModUpdater {

    override fun updateMod(featuredMod: FeaturedMod, version: Int?): CompletableFuture<PatchResult> {
        return CompletableFuture.completedFuture(null)
    }

    override fun canUpdate(featuredMod: FeaturedMod): Boolean {
        return true
    }
}
