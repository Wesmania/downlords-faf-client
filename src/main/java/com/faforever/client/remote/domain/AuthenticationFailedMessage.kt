package com.faforever.client.remote.domain

class AuthenticationFailedMessage : FafServerMessage(FafServerMessageType.AUTHENTICATION_FAILED) {

    var text: String? = null
}
