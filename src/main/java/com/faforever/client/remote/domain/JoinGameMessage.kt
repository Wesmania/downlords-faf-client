package com.faforever.client.remote.domain

class JoinGameMessage(uid: Int, password: String) : ClientMessage(ClientMessageType.JOIN_GAME) {

    var uid: Int? = null
    var password: String? = null

    init {
        this.uid = uid
        this.password = password
    }
}
