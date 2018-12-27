package com.faforever.client.remote.domain

import com.faforever.client.fa.relay.GpgClientCommand
import com.faforever.client.fa.relay.GpgGameMessage

import java.util.Collections

class GameEndedMessage : GpgGameMessage(GpgClientCommand.GAME_STATE, listOf<Any>("Ended"))
