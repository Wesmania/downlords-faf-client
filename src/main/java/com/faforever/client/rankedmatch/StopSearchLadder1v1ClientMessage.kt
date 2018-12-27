package com.faforever.client.rankedmatch

import com.faforever.client.remote.domain.ClientMessageType

class StopSearchLadder1v1ClientMessage : MatchMakerClientMessage(ClientMessageType.GAME_MATCH_MAKING) {
    init {
        state = "stop"
        mod = "ladder1v1"
    }
}
