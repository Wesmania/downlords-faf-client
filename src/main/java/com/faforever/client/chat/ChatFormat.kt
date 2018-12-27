package com.faforever.client.chat

import lombok.Getter

enum class ChatFormat private constructor(@field:Getter
                                          private val i18nKey: String) {
    EXTENDED("settings.chat.chatFormat.extended"),
    COMPACT("settings.chat.chatFormat.compact")
}
