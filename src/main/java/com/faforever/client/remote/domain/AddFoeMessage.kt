package com.faforever.client.remote.domain

class AddFoeMessage(val foe: Int) : ClientMessage(ClientMessageType.SOCIAL_ADD)
