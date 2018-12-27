package com.faforever.client.chat.avatar

import javafx.scene.image.Image
import java.util.concurrent.CompletableFuture

interface AvatarService {

    val availableAvatars: CompletableFuture<List<AvatarBean>>

    fun loadAvatar(avatarUrl: String): Image

    fun changeAvatar(avatar: AvatarBean)
}
