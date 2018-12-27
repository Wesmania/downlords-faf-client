package com.faforever.client.remote.domain

import com.faforever.client.rankedmatch.MatchmakerMessage
import com.faforever.client.remote.UpdatedAchievementsMessage

import java.util.HashMap

enum class FafServerMessageType private constructor(override val string: String, private val type: Class<out FafServerMessage>) : ServerMessageType {
    WELCOME("welcome", LoginMessage::class.java),
    SESSION("session", SessionMessage::class.java),
    GAME_INFO("game_info", GameInfoMessage::class.java),
    PLAYER_INFO("player_info", PlayersMessage::class.java),
    GAME_LAUNCH("game_launch", GameLaunchMessage::class.java),
    MATCHMAKER_INFO("matchmaker_info", MatchmakerMessage::class.java),
    SOCIAL("social", SocialMessage::class.java),
    AUTHENTICATION_FAILED("authentication_failed", AuthenticationFailedMessage::class.java),
    UPDATED_ACHIEVEMENTS("updated_achievements", UpdatedAchievementsMessage::class.java),
    NOTICE("notice", NoticeMessage::class.java),
    ICE_SERVERS("ice_servers", IceServersServerMessage::class.java),
    AVATAR("avatar", AvatarMessage::class.java);

    override fun <T : ServerMessage> getType(): Class<T> {
        return type as Class<T>
    }

    companion object {

        private val fromString: MutableMap<String, FafServerMessageType>

        init {
            fromString = HashMap(values().size, 1f)
            for (fafServerMessageType in values()) {
                fromString[fafServerMessageType.string] = fafServerMessageType
            }
        }

        fun fromString(string: String): FafServerMessageType {
            return fromString[string]
        }
    }

}
