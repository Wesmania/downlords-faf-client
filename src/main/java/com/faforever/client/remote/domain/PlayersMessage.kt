package com.faforever.client.remote.domain

class PlayersMessage : FafServerMessage(FafServerMessageType.PLAYER_INFO) {

    var players: List<Player>? = null
}
