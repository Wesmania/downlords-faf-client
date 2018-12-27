package com.faforever.client.remote.domain

import com.faforever.client.fa.relay.GpgClientCommand
import com.faforever.client.fa.relay.GpgGameMessage

import java.util.Arrays

class IceMessage(remotePlayerId: Int, message: Any) : GpgGameMessage(GpgClientCommand.ICE_MESSAGE, Arrays.asList(remotePlayerId, message))
