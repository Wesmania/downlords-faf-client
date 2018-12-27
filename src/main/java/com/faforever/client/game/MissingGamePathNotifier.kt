package com.faforever.client.game

import com.faforever.client.i18n.I18n
import com.faforever.client.notification.Action
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.Severity
import com.faforever.client.preferences.event.MissingGamePathEvent
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.util.Collections

@Component
class MissingGamePathNotifier @Inject
constructor(private val eventBus: EventBus, private val i18n: I18n, private val notificationService: NotificationService) {

    @PostConstruct
    fun postConstruct() {
        eventBus.register(this)
    }

    @Subscribe
    fun onMissingGamePathEvent(event: MissingGamePathEvent) {
        val actions = listOf(Action(i18n.get("missingGamePath.locate")) { chooseEvent -> eventBus.post(GameDirectoryChooseEvent()) })
        val notificationText = i18n.get("missingGamePath.notification")

        if (event.isImmediateUserActionRequired()) {
            notificationService.addNotification(ImmediateNotification(notificationText, notificationText, Severity.WARN, actions))
        } else {
            notificationService.addNotification(PersistentNotification(notificationText, Severity.WARN, actions))
        }
    }
}
