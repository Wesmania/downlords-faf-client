package com.faforever.client.player

import com.faforever.client.audio.AudioService
import com.faforever.client.chat.InitiatePrivateChatEvent
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.NavigationItem
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.TransientNotification
import com.faforever.client.preferences.NotificationsPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.util.IdenticonUtil
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject

/**
 * Displays a notification whenever a friend comes online (if enabled in settings).
 */
@Component
class FriendOnlineNotifier @Inject
constructor(private val notificationService: NotificationService, private val i18n: I18n, private val eventBus: EventBus,
            private val audioService: AudioService, private val playerService: PlayerService, private val preferencesService: PreferencesService) {

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onUserOnline(event: PlayerOnlineEvent) {
        val notification = preferencesService.preferences!!.notification
        val player = event.getPlayer()

        if (player.getSocialStatus() != SocialStatus.FRIEND) {
            return
        }

        if (notification.isFriendOnlineSoundEnabled) {
            audioService.playFriendOnlineSound()
        }

        if (notification.isFriendOnlineToastEnabled) {
            notificationService.addNotification(
                    TransientNotification(
                            i18n.get("friend.nowOnlineNotification.title", player.getUsername()),
                            i18n.get("friend.nowOnlineNotification.action"),
                            IdenticonUtil.createIdenticon(player.getId())
                    ) { actionEvent ->
                        eventBus.post(NavigateEvent(NavigationItem.CHAT))
                        eventBus.post(InitiatePrivateChatEvent(player.getUsername()))
                    })
        }
    }
}
