package com.faforever.client.chat

import com.faforever.client.net.ConnectionState
import javafx.beans.property.ReadOnlyIntegerProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.collections.MapChangeListener

import java.util.concurrent.CompletableFuture

interface ChatService {

    val defaultChannelName: String

    fun connect()

    fun disconnect()

    fun sendMessageInBackground(target: String, message: String): CompletableFuture<String>

    /**
     * Gets the list of chat users for the given channel as soon as it is available.
     *
     * **IMPORTANT:** All
     * operations on the returned list must be synchronized, even iteration. Use the map as monitor.
     */
    fun getOrCreateChannel(channelName: String): Channel

    fun getOrCreateChatUser(username: String, channel: String, isModerator: Boolean): ChatChannelUser

    fun addUsersListener(channelName: String, listener: MapChangeListener<String, ChatChannelUser>)

    fun addChatUsersByNameListener(listener: MapChangeListener<String, ChatChannelUser>)

    fun addChannelsListener(listener: MapChangeListener<String, Channel>)

    fun removeUsersListener(channelName: String, listener: MapChangeListener<String, ChatChannelUser>)

    fun leaveChannel(channelName: String)

    fun sendActionInBackground(target: String, action: String): CompletableFuture<String>

    fun joinChannel(channelName: String)

    fun isDefaultChannel(channelName: String): Boolean

    fun close()

    fun connectionStateProperty(): ReadOnlyObjectProperty<ConnectionState>

    fun reconnect()

    fun whois(username: String)

    /**
     * Increase or decrease the number of unread messages.
     *
     * @param delta a positive or negative number
     */
    fun incrementUnreadMessagesCount(delta: Int)

    fun unreadMessagesCount(): ReadOnlyIntegerProperty

    fun getChatUser(username: String, channelName: String): ChatChannelUser
}
