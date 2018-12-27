package com.faforever.client.remote.domain

import com.faforever.client.fa.relay.GpgServerMessage
import com.faforever.client.fa.relay.GpgServerMessageType

class IceServerMessage : GpgServerMessage(GpgServerMessageType.ICE_MESSAGE, 2) {

    val sender: Int
        get() = getInt(SENDER_INDEX)

    val record: Map<String, Any>
        get() = getObject(RECORD_INDEX)

    companion object {
        private val SENDER_INDEX = 0
        private val RECORD_INDEX = 1
    }
}
