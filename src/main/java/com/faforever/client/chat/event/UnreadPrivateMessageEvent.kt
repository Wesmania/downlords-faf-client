package com.faforever.client.chat.event

import com.faforever.client.chat.ChatMessage
import lombok.Data

@Data
class UnreadPrivateMessageEvent {
    private val message: ChatMessage? = null
}
