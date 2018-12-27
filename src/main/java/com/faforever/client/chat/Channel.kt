package com.faforever.client.chat

import com.faforever.client.fx.JavaFxUtil
import javafx.beans.property.ReadOnlySetWrapper
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.collections.ObservableSet

import java.util.ArrayList
import java.util.Collections
import java.util.Optional

class Channel(val name: String) {

    private val users: ObservableMap<String, ChatChannelUser>
    private val topic: StringProperty
    private val moderators: ObservableSet<String>

    init {
        users = FXCollections.synchronizedObservableMap(FXCollections.observableHashMap())
        moderators = FXCollections.observableSet()
        topic = SimpleStringProperty()
    }

    fun getTopic(): String {
        return topic.get()
    }

    fun setTopic(topic: String) {
        this.topic.set(topic)
    }

    fun topicProperty(): StringProperty {
        return topic
    }

    fun removeUser(username: String): ChatChannelUser {
        return users.remove(username)
    }

    fun addUsers(users: List<ChatChannelUser>) {
        users.forEach(Consumer<ChatChannelUser> { this.addUser(it) })
    }

    fun addUser(user: ChatChannelUser) {
        if (moderators.contains(user.username)) {
            user.isModerator = true
        }
        users[user.username] = user
    }

    fun clearUsers() {
        users.clear()
    }

    fun addUsersListeners(listener: MapChangeListener<String, ChatChannelUser>) {
        JavaFxUtil.addListener(users, listener)
    }

    fun removeUserListener(listener: MapChangeListener<String, ChatChannelUser>) {
        users.removeListener(listener)
    }

    fun addModerator(username: String) {
        Optional.ofNullable(users[username]).ifPresent { user -> user.isModerator = true }
        moderators.add(username)
    }

    fun getModerators(): ReadOnlySetWrapper<String> {
        return ReadOnlySetWrapper(moderators)
    }

    /**
     * Returns an unmodifiable copy of the current users.
     */
    fun getUsers(): List<ChatChannelUser> {
        return Collections.unmodifiableList(ArrayList(users.values))
    }

    fun getUser(username: String): ChatChannelUser {
        return users[username]
    }
}
