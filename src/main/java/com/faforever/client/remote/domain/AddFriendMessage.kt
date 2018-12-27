package com.faforever.client.remote.domain

class AddFriendMessage(val friend: Int) : ClientMessage(ClientMessageType.SOCIAL_ADD)
