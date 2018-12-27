package com.faforever.client.game

import com.faforever.client.remote.domain.GameInfoMessage

interface OnGameAddedListener {

    fun onGameAdded(gameInfoMessage: GameInfoMessage)
}
