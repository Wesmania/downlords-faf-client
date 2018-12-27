package com.faforever.client.game

import lombok.Value

/**
 * Fired whenever the information of a game has changed.
 */
@Value
class GameUpdatedEvent {
    private val game: Game? = null
}
