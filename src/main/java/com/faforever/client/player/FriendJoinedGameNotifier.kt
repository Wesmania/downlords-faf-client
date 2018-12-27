package com.faforever.client.player

import com.faforever.client.audio.AudioService
import com.faforever.client.game.Game
import com.faforever.client.game.JoinGameHelper
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.TransientNotification
import com.faforever.client.player.event.FriendJoinedGameEvent
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.util.IdenticonUtil
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject

/**
 * Displays a notification whenever a friend joins a preferences (if enabled in settings).
 */
@Component
class FriendJoinedGameNotifier @Inject
constructor(private val notificationService: NotificationService, private val i18n: I18n, private val eventBus: EventBus,
            private val joinGameHelper: JoinGameHelper, private val preferencesService: PreferencesService,
            private val audioService: AudioService) {

    @PostConstruct
    internal fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onFriendJoinedGame(event: FriendJoinedGameEvent) {
        val player = event.player
        val game = event.game

        audioService.playFriendJoinsGameSound()

        if (preferencesService.preferences!!.notification.isFriendJoinsGameToastEnabled) {
            notificationService.addNotification(TransientNotification(
                    i18n.get("friend.joinedGameNotification.title", player.username, game.title),
                    i18n.get("friend.joinedGameNotification.action"),
                    IdenticonUtil.createIdenticon(player.id)
            ) { event1 -> joinGameHelper.join(player.game) })
        }
    }
}
