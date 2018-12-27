package com.faforever.client.preferences

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty

class NotificationsPrefs {

    private val soundsEnabled: BooleanProperty
    private val transientNotificationsEnabled: BooleanProperty
    private val mentionSoundEnabled: BooleanProperty
    private val infoSoundEnabled: BooleanProperty
    private val warnSoundEnabled: BooleanProperty
    private val errorSoundEnabled: BooleanProperty
    private val friendOnlineToastEnabled: BooleanProperty
    private val friendOfflineToastEnabled: BooleanProperty
    private val ladder1v1ToastEnabled: BooleanProperty
    private val friendOnlineSoundEnabled: BooleanProperty
    private val friendOfflineSoundEnabled: BooleanProperty
    private val friendJoinsGameSoundEnabled: BooleanProperty
    private val friendPlaysGameSoundEnabled: BooleanProperty
    private val friendPlaysGameToastEnabled: BooleanProperty
    private val privateMessageSoundEnabled: BooleanProperty
    private val privateMessageToastEnabled: BooleanProperty
    private val friendJoinsGameToastEnabled: BooleanProperty
    private val notifyOnAtMentionOnlyEnabled: BooleanProperty
    private val afterGameReviewEnabled: BooleanProperty
    private val toastPosition: ObjectProperty<ToastPosition>
    private val toastScreen: IntegerProperty
    private val toastDisplayTime: IntegerProperty

    var isSoundsEnabled: Boolean
        get() = soundsEnabled.get()
        set(soundsEnabled) = this.soundsEnabled.set(soundsEnabled)

    var isTransientNotificationsEnabled: Boolean
        get() = transientNotificationsEnabled.get()
        set(transientNotificationsEnabled) = this.transientNotificationsEnabled.set(transientNotificationsEnabled)

    var isMentionSoundEnabled: Boolean
        get() = mentionSoundEnabled.get()
        set(mentionSoundEnabled) = this.mentionSoundEnabled.set(mentionSoundEnabled)

    var isInfoSoundEnabled: Boolean
        get() = infoSoundEnabled.get()
        set(infoSoundEnabled) = this.infoSoundEnabled.set(infoSoundEnabled)

    var isWarnSoundEnabled: Boolean
        get() = warnSoundEnabled.get()
        set(warnSoundEnabled) = this.warnSoundEnabled.set(warnSoundEnabled)

    var isErrorSoundEnabled: Boolean
        get() = errorSoundEnabled.get()
        set(errorSoundEnabled) = this.errorSoundEnabled.set(errorSoundEnabled)

    var isFriendOnlineToastEnabled: Boolean
        get() = friendOnlineToastEnabled.get()
        set(friendOnlineToastEnabled) = this.friendOnlineToastEnabled.set(friendOnlineToastEnabled)

    var isFriendOfflineToastEnabled: Boolean
        get() = friendOfflineToastEnabled.get()
        set(friendOfflineToastEnabled) = this.friendOfflineToastEnabled.set(friendOfflineToastEnabled)

    var isFriendOnlineSoundEnabled: Boolean
        get() = friendOnlineSoundEnabled.get()
        set(friendOnlineSoundEnabled) = this.friendOnlineSoundEnabled.set(friendOnlineSoundEnabled)

    var isFriendOfflineSoundEnabled: Boolean
        get() = friendOfflineSoundEnabled.get()
        set(friendOfflineSoundEnabled) = this.friendOfflineSoundEnabled.set(friendOfflineSoundEnabled)

    var isFriendJoinsGameSoundEnabled: Boolean
        get() = friendJoinsGameSoundEnabled.get()
        set(friendJoinsGameSoundEnabled) = this.friendJoinsGameSoundEnabled.set(friendJoinsGameSoundEnabled)

    var isFriendPlaysGameSoundEnabled: Boolean
        get() = friendPlaysGameSoundEnabled.get()
        set(friendPlaysGameSoundEnabled) = this.friendPlaysGameSoundEnabled.set(friendPlaysGameSoundEnabled)

    var isFriendPlaysGameToastEnabled: Boolean
        get() = friendPlaysGameToastEnabled.get()
        set(friendPlaysGameToastEnabled) = this.friendPlaysGameToastEnabled.set(friendPlaysGameToastEnabled)

    var isPrivateMessageSoundEnabled: Boolean
        get() = privateMessageSoundEnabled.get()
        set(privateMessageSoundEnabled) = this.privateMessageSoundEnabled.set(privateMessageSoundEnabled)

    var isPrivateMessageToastEnabled: Boolean
        get() = privateMessageToastEnabled.get()
        set(privateMessageToastEnabled) = this.privateMessageToastEnabled.set(privateMessageToastEnabled)

    var isFriendJoinsGameToastEnabled: Boolean
        get() = friendJoinsGameToastEnabled.get()
        set(friendJoinsGameToastEnabled) = this.friendJoinsGameToastEnabled.set(friendJoinsGameToastEnabled)

    var isAfterGameReviewEnabled: Boolean
        get() = afterGameReviewEnabled.get()
        set(afterGameReviewEnabled) = this.afterGameReviewEnabled.set(afterGameReviewEnabled)

    init {
        soundsEnabled = SimpleBooleanProperty(true)
        mentionSoundEnabled = SimpleBooleanProperty(true)
        infoSoundEnabled = SimpleBooleanProperty(true)
        warnSoundEnabled = SimpleBooleanProperty(true)
        errorSoundEnabled = SimpleBooleanProperty(true)
        transientNotificationsEnabled = SimpleBooleanProperty(true)
        toastPosition = SimpleObjectProperty(ToastPosition.BOTTOM_RIGHT)
        friendOnlineToastEnabled = SimpleBooleanProperty(true)
        friendOfflineToastEnabled = SimpleBooleanProperty(true)
        ladder1v1ToastEnabled = SimpleBooleanProperty(true)
        friendOnlineSoundEnabled = SimpleBooleanProperty(true)
        friendOfflineSoundEnabled = SimpleBooleanProperty(true)
        friendJoinsGameSoundEnabled = SimpleBooleanProperty(true)
        friendPlaysGameSoundEnabled = SimpleBooleanProperty(true)
        friendPlaysGameToastEnabled = SimpleBooleanProperty(true)
        friendJoinsGameToastEnabled = SimpleBooleanProperty(true)
        privateMessageSoundEnabled = SimpleBooleanProperty(true)
        privateMessageToastEnabled = SimpleBooleanProperty(true)
        notifyOnAtMentionOnlyEnabled = SimpleBooleanProperty(false)
        toastScreen = SimpleIntegerProperty(0)
        toastDisplayTime = SimpleIntegerProperty(5000)
        afterGameReviewEnabled = SimpleBooleanProperty(true)
    }

    fun soundsEnabledProperty(): BooleanProperty {
        return soundsEnabled
    }

    fun transientNotificationsEnabledProperty(): BooleanProperty {
        return transientNotificationsEnabled
    }

    fun mentionSoundEnabledProperty(): BooleanProperty {
        return mentionSoundEnabled
    }

    fun infoSoundEnabledProperty(): BooleanProperty {
        return infoSoundEnabled
    }

    fun warnSoundEnabledProperty(): BooleanProperty {
        return warnSoundEnabled
    }

    fun errorSoundEnabledProperty(): BooleanProperty {
        return errorSoundEnabled
    }

    fun friendOnlineToastEnabledProperty(): BooleanProperty {
        return friendOnlineToastEnabled
    }

    fun friendOfflineToastEnabledProperty(): BooleanProperty {
        return friendOfflineToastEnabled
    }

    fun getLadder1v1ToastEnabled(): Boolean {
        return ladder1v1ToastEnabled.get()
    }

    fun setLadder1v1ToastEnabled(ladder1v1ToastEnabled: Boolean) {
        this.ladder1v1ToastEnabled.set(ladder1v1ToastEnabled)
    }

    fun ladder1v1ToastEnabledProperty(): BooleanProperty {
        return ladder1v1ToastEnabled
    }

    fun friendOnlineSoundEnabledProperty(): BooleanProperty {
        return friendOnlineSoundEnabled
    }

    fun friendOfflineSoundEnabledProperty(): BooleanProperty {
        return friendOfflineSoundEnabled
    }

    fun friendJoinsGameSoundEnabledProperty(): BooleanProperty {
        return friendJoinsGameSoundEnabled
    }

    fun friendPlaysGameSoundEnabledProperty(): BooleanProperty {
        return friendPlaysGameSoundEnabled
    }

    fun friendPlaysGameToastEnabledProperty(): BooleanProperty {
        return friendPlaysGameToastEnabled
    }

    fun privateMessageSoundEnabledProperty(): BooleanProperty {
        return privateMessageSoundEnabled
    }

    fun privateMessageToastEnabledProperty(): BooleanProperty {
        return privateMessageToastEnabled
    }

    fun friendJoinsGameToastEnabledProperty(): BooleanProperty {
        return friendJoinsGameToastEnabled
    }

    fun getToastPosition(): ToastPosition {
        return toastPosition.get()
    }

    fun setToastPosition(toastPosition: ToastPosition) {
        this.toastPosition.set(toastPosition)
    }

    fun toastPositionProperty(): ObjectProperty<ToastPosition> {
        return toastPosition
    }

    fun getToastScreen(): Int {
        return toastScreen.get()
    }

    fun setToastScreen(toastScreen: Int) {
        this.toastScreen.set(toastScreen)
    }

    fun toastScreenProperty(): IntegerProperty {
        return toastScreen
    }

    fun getToastDisplayTime(): Int {
        return toastDisplayTime.get()
    }

    fun setToastDisplayTime(toastDisplayTime: Int) {
        this.toastDisplayTime.set(toastDisplayTime)
    }

    fun toastDisplayTimeProperty(): IntegerProperty {
        return toastDisplayTime
    }

    fun getNotifyOnAtMentionOnlyEnabled(): Boolean {
        return notifyOnAtMentionOnlyEnabled.get()
    }

    fun notifyOnAtMentionOnlyEnabledProperty(): BooleanProperty {
        return notifyOnAtMentionOnlyEnabled
    }

    fun afterGameReviewEnabledProperty(): BooleanProperty {
        return afterGameReviewEnabled
    }
}
