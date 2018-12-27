package com.faforever.client.remote.domain

class GameLaunchMessage : FafServerMessage(FafServerMessageType.GAME_LAUNCH) {

    /**
     * Stores game launch arguments, like "/ratingcolor d8d8d8d8", "/numgames 236".
     */
    var args: List<String>? = null
    var uid: Int = 0
    var mod: String? = null
    var mapname: String? = null
}
