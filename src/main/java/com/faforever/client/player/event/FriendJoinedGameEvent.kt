package com.faforever.client.player.event

import com.faforever.client.game.Game
import com.faforever.client.player.Player

class FriendJoinedGameEvent(val player: Player, val game: Game) {

    override fun equals(obj: Any?): Boolean {
        return (obj is FriendJoinedGameEvent
                && obj.player == player
                && obj.game == game)
    }
}
