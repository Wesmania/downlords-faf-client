package com.faforever.client.remote.domain

class SocialMessage : FafServerMessage() {

    val friends: List<Int>? = null
    val foes: List<Int>? = null
    /**
     * List of channel names to join.
     */
    var channels: List<String>? = null
}
