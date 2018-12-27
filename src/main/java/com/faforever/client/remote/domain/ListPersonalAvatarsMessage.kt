package com.faforever.client.remote.domain

class ListPersonalAvatarsMessage : ClientMessage(ClientMessageType.AVATAR) {

    private val action: String

    init {
        this.action = "list_avatar"
    }
}
