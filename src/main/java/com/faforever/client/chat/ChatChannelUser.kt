package com.faforever.client.chat

import com.faforever.client.player.Player
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.paint.Color
import lombok.ToString

import java.time.Instant
import java.util.HashSet
import java.util.Optional

/**
 * Represents a chat user within a channel. If a user is in multiple channels, one instance per channel needs to be
 * created since e.g. the `isModerator` flag is specific to the channel.
 */
@ToString
class ChatChannelUser @JvmOverloads internal constructor(username: String, color: Color, moderator: Boolean, player: Player? = null) {

    private val username: StringProperty
    private val moderator: BooleanProperty
    private val color: ObjectProperty<Color>
    private val player: ObjectProperty<Player>
    private val lastActive: ObjectProperty<Instant>

    var isModerator: Boolean
        get() = moderator.get()
        set(moderator) = this.moderator.set(moderator)

    internal// Nothing to do
    val chatUserCategories: Set<ChatUserCategory>
        get() {
            val userCategories = HashSet<ChatUserCategory>()

            val playerOptional = Optional.ofNullable(player.get())
            if (!playerOptional.isPresent) {
                userCategories.add(ChatUserCategory.CHAT_ONLY)
            } else {
                when (playerOptional.get().socialStatus) {
                    SocialStatus.FRIEND -> userCategories.add(ChatUserCategory.FRIEND)
                    SocialStatus.FOE -> userCategories.add(ChatUserCategory.FOE)
                    SocialStatus.OTHER, SocialStatus.SELF -> userCategories.add(ChatUserCategory.OTHER)
                }
            }

            if (moderator.get()) {
                userCategories.add(ChatUserCategory.MODERATOR)
            }

            return userCategories
        }

    init {
        this.username = SimpleStringProperty(username)
        this.moderator = SimpleBooleanProperty(moderator)
        this.color = SimpleObjectProperty(color)
        this.player = SimpleObjectProperty(player)
        this.lastActive = SimpleObjectProperty()
    }

    fun getPlayer(): Optional<Player> {
        return Optional.ofNullable(player.get())
    }

    fun setPlayer(player: Player) {
        this.player.set(player)
    }

    fun playerProperty(): ObjectProperty<Player> {
        return player
    }

    fun getColor(): Color {
        return color.get()
    }

    fun setColor(color: Color) {
        this.color.set(color)
    }

    fun colorProperty(): ObjectProperty<Color> {
        return color
    }

    fun getUsername(): String {
        return username.get()
    }

    fun usernameProperty(): StringProperty {
        return username
    }

    override fun hashCode(): Int {
        return username.get().hashCode()
    }

    fun moderatorProperty(): BooleanProperty {
        return moderator
    }

    fun getLastActive(): Instant {
        return lastActive.get()
    }

    fun setLastActive(lastActive: Instant) {
        this.lastActive.set(lastActive)
    }

    fun lastActiveProperty(): ObjectProperty<Instant> {
        return lastActive
    }

    override fun equals(obj: Any?): Boolean {
        return (obj != null
                && obj.javaClass == this.javaClass
                && username.get().equals((obj as ChatChannelUser).username.get(), ignoreCase = true))
    }
}
