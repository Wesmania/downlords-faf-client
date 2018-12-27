package com.faforever.client.main.event

import com.faforever.client.replay.Replay
import lombok.Data

@Data
class ShowReplayEvent : OpenOnlineReplayVaultEvent() {
    private val replay: Replay? = null
}
