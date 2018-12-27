package com.faforever.client.game

import lombok.Value

/**
 * Fired whenever the information of a game has been removed.
 */
@Value
class GameRemovedEvent {
    private val game: Game? = null
}
