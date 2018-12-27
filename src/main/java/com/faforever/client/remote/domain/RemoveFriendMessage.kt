package com.faforever.client.remote.domain

class RemoveFriendMessage(val friend: Int) : ClientMessage(ClientMessageType.SOCIAL_REMOVE)
