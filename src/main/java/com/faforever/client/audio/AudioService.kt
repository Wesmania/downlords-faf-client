package com.faforever.client.audio

import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.preferences.NotificationsPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import javafx.scene.media.AudioClip
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.io.IOException

@Lazy
@Service
class AudioService @Inject
constructor(private val preferencesService: PreferencesService, private val audioClipPlayer: AudioClipPlayer, private val uiService: UiService) {

    private var chatMentionSound: AudioClip? = null
    private var achievementUnlockedSound: AudioClip? = null
    private var errorNotificationSound: AudioClip? = null
    private var infoNotificationSound: AudioClip? = null
    private var warnNotificationSound: AudioClip? = null
    private var privateMessageSound: AudioClip? = null
    private val friendOnlineSound: AudioClip? = null
    private val friendOfflineSound: AudioClip? = null
    private val friendJoinsGameSound: AudioClip? = null
    private val friendPlaysGameSound: AudioClip? = null

    private var playSounds: Boolean = false
    private var notificationsPrefs: NotificationsPrefs? = null

    @PostConstruct
    @Throws(IOException::class)
    internal fun postConstruct() {
        notificationsPrefs = preferencesService.preferences!!.notification
        JavaFxUtil.addListener(notificationsPrefs!!.soundsEnabledProperty()
        ) { observable, oldValue, newValue -> playSounds = newValue!! }
        playSounds = notificationsPrefs!!.isSoundsEnabled

        loadSounds()
    }

    @Throws(IOException::class)
    private fun loadSounds() {
        achievementUnlockedSound = loadSound(ACHIEVEMENT_UNLOCKED_SOUND)
        infoNotificationSound = loadSound(INFO_SOUND)
        errorNotificationSound = loadSound(INFO_SOUND)
        warnNotificationSound = loadSound(INFO_SOUND)
        chatMentionSound = loadSound(MENTION_SOUND)
        privateMessageSound = loadSound(PRIVATE_MESSAGE_SOUND)
        // TODO implement
        //    friendOnlineSound = loadSound(FRIEND_ONLINE_SOUND);
        //    friendOfflineSound = loadSound(FRIEND_OFFLINE_SOUND);
        //    friendJoinsGameSound = loadSound(FRIEND_JOINS_GAME_SOUND);
        //    friendPlaysGameSound = loadSound(FRIEND_PLAYS_GAME_SOUND);
    }

    @Throws(IOException::class)
    private fun loadSound(sound: String): AudioClip {
        return AudioClip(uiService.getThemeFileUrl(sound).toString())
    }


    fun playChatMentionSound() {
        if (!notificationsPrefs!!.isMentionSoundEnabled) {
            return
        }
        playSound(chatMentionSound)
    }


    fun playPrivateMessageSound() {
        if (!notificationsPrefs!!.isPrivateMessageSoundEnabled) {
            return
        }
        playSound(privateMessageSound)
    }


    fun playInfoNotificationSound() {
        if (!notificationsPrefs!!.isInfoSoundEnabled) {
            return
        }
        playSound(infoNotificationSound)
    }


    fun playWarnNotificationSound() {
        if (!notificationsPrefs!!.isWarnSoundEnabled) {
            return
        }
        playSound(warnNotificationSound)
    }


    fun playErrorNotificationSound() {
        if (!notificationsPrefs!!.isErrorSoundEnabled) {
            return
        }
        playSound(errorNotificationSound)
    }


    fun playAchievementUnlockedSound() {
        playSound(achievementUnlockedSound)
    }


    fun playFriendOnlineSound() {
        if (!notificationsPrefs!!.isFriendOnlineSoundEnabled) {
            return
        }
        // FIXME implement
        //    playSound(friendOnlineSound);
    }


    fun playFriendOfflineSound() {
        if (!notificationsPrefs!!.isFriendOfflineSoundEnabled) {
            return
        }
        // FIXME implement
        //    playSound(friendOfflineSound);
    }


    fun playFriendJoinsGameSound() {
        if (!notificationsPrefs!!.isFriendJoinsGameSoundEnabled) {
            return
        }
        // FIXME implement
        //    playSound(friendJoinsGameSound);
    }


    fun playFriendPlaysGameSound() {
        if (!notificationsPrefs!!.isFriendPlaysGameSoundEnabled) {
            return
        }
        playSound(friendPlaysGameSound)
    }

    private fun playSound(audioClip: AudioClip?) {
        if (!playSounds) {
            return
        }
        audioClipPlayer.playSound(audioClip)
    }

    companion object {

        private val ACHIEVEMENT_UNLOCKED_SOUND = "theme/sounds/achievement_unlocked.mp3"
        private val INFO_SOUND = "theme/sounds/info.mp3"
        private val MENTION_SOUND = "theme/sounds/mention.mp3"
        private val PRIVATE_MESSAGE_SOUND = "theme/sounds/pm.mp3"
    }
}
