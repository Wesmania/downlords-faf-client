package com.faforever.client.remote.domain

class RemoveFoeMessage(val foe: Int) : ClientMessage(ClientMessageType.SOCIAL_REMOVE)
