package com.faforever.client.chat

import lombok.Data

import java.time.Instant
import java.util.Objects

@Data
class ChatMessage @JvmOverloads constructor(private val source: String, // TODO change to LocalTime?
                                            private val time: Instant, private val username: String, private val message: String, private val action: Boolean = false) {

    val isPrivate: Boolean
        get() = !Objects.toString(source, "").startsWith("#")

}
/**
 * @param source the name of the message source/target - either a channel or an username.
 * @param username the user who sent the message
 */
