package com.faforever.client.chat

internal enum class ChatUserCategory private constructor(val i18nKey: String) {
    MODERATOR("chat.category.moderators"),
    FRIEND("chat.category.friends"),
    OTHER("chat.category.others"),
    CHAT_ONLY("chat.category.chatOnly"),
    FOE("chat.category.foes")
}
