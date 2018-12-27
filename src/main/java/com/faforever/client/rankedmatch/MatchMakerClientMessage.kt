package com.faforever.client.rankedmatch

import com.faforever.client.remote.domain.ClientMessage
import com.faforever.client.remote.domain.ClientMessageType

open class MatchMakerClientMessage(command: ClientMessageType) : ClientMessage(command) {

    var mod: String
    var state: String
}
