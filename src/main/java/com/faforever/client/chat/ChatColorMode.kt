package com.faforever.client.chat

enum class ChatColorMode private constructor(val i18nKey: String) {
    DEFAULT("settings.chat.colorMode.default"),
    CUSTOM("settings.chat.colorMode.custom"),
    RANDOM("settings.chat.colorMode.random")

}
