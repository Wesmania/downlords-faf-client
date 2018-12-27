package com.faforever.client.remote.domain

class LoginMessage : FafServerMessage(FafServerMessageType.WELCOME) {

    var id: Int = 0
    var login: String? = null
}
