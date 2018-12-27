package com.faforever.client.player

import lombok.Value

@Value
class PlayerOnlineEvent {
    internal var player: Player? = null
}
