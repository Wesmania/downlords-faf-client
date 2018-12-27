package com.faforever.client.game

import lombok.Value

/**
 * Fired whenever the information of a game has been added.
 */
@Value
class GameAddedEvent {
    private val game: Game? = null
}
