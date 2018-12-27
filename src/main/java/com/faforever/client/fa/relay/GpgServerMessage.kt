package com.faforever.client.fa.relay

import com.faforever.client.remote.domain.MessageTarget
import com.faforever.client.remote.domain.SerializableMessage
import com.faforever.client.remote.domain.ServerMessage

import java.util.ArrayList
import java.util.Collections

/**
 * Represents a message received from the relay server (deserialized from JSON).
 */
open class GpgServerMessage protected constructor(override val messageType: GpgServerMessageType, numberOfArgs: Int) : SerializableMessage, ServerMessage {
    override var target: MessageTarget? = null
    private val args: MutableList<Any>

    override val stringsToMask: Collection<String>
        get() = emptyList()

    init {
        this.args = ArrayList(Collections.nCopies<Any>(numberOfArgs, null))
    }

    protected fun setValue(index: Int, value: Any) {
        args[index] = value
    }

    protected fun getInt(index: Int): Int {
        return (args[index] as Number).toInt()
    }

    protected fun getBoolean(index: Int): Boolean {
        return args[index] as Boolean
    }

    protected fun getString(index: Int): String {
        return args[index] as String
    }

    protected fun <T> getObject(index: Int): T {
        return args[index] as T
    }
}
