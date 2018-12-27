package com.faforever.client.player

enum class SocialStatus private constructor(val cssClass: String) {
    FRIEND("friend"),
    FOE("foe"),
    OTHER("other"),
    SELF("self")
}
