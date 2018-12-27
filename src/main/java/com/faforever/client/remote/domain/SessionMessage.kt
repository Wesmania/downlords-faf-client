package com.faforever.client.remote.domain

class SessionMessage : FafServerMessage(FafServerMessageType.SESSION) {

    var session: Long = 0
}
