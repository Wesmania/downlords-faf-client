package com.faforever.client.fa.relay


import com.faforever.client.remote.domain.MessageTarget
import com.faforever.client.remote.domain.SerializableMessage
import java.util.Collections


open class GpgGameMessage(private val command: String, val args: List<Any>) : SerializableMessage {
    var target = MessageTarget.GAME

    override val stringsToMask: Collection<String>
        get() = emptyList()

    constructor(command: GpgClientCommand, args: List<Any>) : this(command.string, args) {}

    fun getCommand(): GpgClientCommand? {
        return GpgClientCommand.fromString(command)
    }

    protected fun getInt(index: Int): Int {
        return (args[index] as Number).toInt()
    }

    protected fun getString(index: Int): String {
        return args[index] as String
    }
}
