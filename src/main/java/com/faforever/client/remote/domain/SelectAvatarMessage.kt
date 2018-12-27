package com.faforever.client.remote.domain

import java.net.URL
import java.util.Objects

class SelectAvatarMessage(url: URL) : ClientMessage(ClientMessageType.AVATAR) {

    private val action: String

    private val avatar: String

    init {
        avatar = Objects.toString(url, "")
        this.action = "select"
    }
}
