package com.faforever.client.chat

import lombok.Value

@Value
class ChatUserCreatedEvent {
    internal var chatChannelUser: ChatChannelUser? = null
}
