package com.faforever.client.remote.domain

import java.util.Collections

class LoginClientMessage(username: String, password: String, session: Long, uniqueId: String, localIp: String) : ClientMessage(ClientMessageType.LOGIN) {

    var login: String? = null
    var password: String? = null
    var session: Long = 0
    var uniqueId: String? = null
    var localIp: String? = null

    override val stringsToMask: Collection<String>
        get() = listOf<String>(password)

    init {
        this.login = username
        this.password = password
        this.session = session
        this.uniqueId = uniqueId
        this.localIp = localIp
    }
}
