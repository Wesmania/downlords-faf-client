package com.faforever.client.player

import com.faforever.client.audio.AudioService
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.TransientNotification
import com.faforever.client.util.IdenticonUtil
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject

/**
 * Displays a notification whenever a friend goes offline (if enabled in settings).
 */
@Component
class FriendOfflineNotifier @Inject
constructor(private val notificationService: NotificationService, private val i18n: I18n, private val eventBus: EventBus,
            private val audioService: AudioService, private val playerService: PlayerService) {

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onUserOnline(event: UserOfflineEvent) {
        val username = event.username
        playerService.getPlayerForUsername(username).ifPresent { player ->
            if (player.socialStatus != SocialStatus.FRIEND) {
                return@playerService.getPlayerForUsername(username).ifPresent
            }

            audioService.playFriendOfflineSound()
            notificationService.addNotification(
                    TransientNotification(
                            i18n.get("friend.nowOfflineNotification.title", username), "",
                            IdenticonUtil.createIdenticon(player.id)
                    ))
        }
    }
}
