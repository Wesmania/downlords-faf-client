package com.faforever.client.chat.avatar

import com.faforever.client.remote.AssetService
import com.faforever.client.remote.FafService
import javafx.scene.image.Image
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

import com.faforever.client.config.CacheNames.AVATARS
import com.github.nocatch.NoCatch.noCatch

@Lazy
@Service
class AvatarServiceImpl @Inject
constructor(private val fafService: FafService, private val assetService: AssetService) : AvatarService {

    override val availableAvatars: CompletableFuture<List<AvatarBean>>
        get() = fafService.availableAvatars

    @Cacheable(AVATARS)
    override fun loadAvatar(avatarUrl: String): Image? {
        return assetService.loadAndCacheImage(noCatch<URL> { URL(avatarUrl) }, Paths.get("avatars"), null)
    }

    override fun changeAvatar(avatar: AvatarBean) {
        fafService.selectAvatar(avatar)
    }
}
