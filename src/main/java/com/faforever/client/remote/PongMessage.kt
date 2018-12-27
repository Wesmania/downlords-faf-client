package com.faforever.client.remote

import com.faforever.client.remote.domain.ClientMessage
import com.faforever.client.remote.domain.ClientMessageType

class PongMessage : ClientMessage(ClientMessageType.PONG)
