package com.faforever.client.notification

import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.reporting.ReportingService
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.TreeSet

import com.faforever.client.notification.Severity.ERROR
import java.util.Collections.singletonList
import javafx.collections.FXCollections.observableSet
import javafx.collections.FXCollections.synchronizedObservableSet


@Lazy
@Service
// TODO instead of being required an called explicitly, this service should listen for application events only
class NotificationService @Inject
constructor(private val reportingService: ReportingService) {

    private val persistentNotifications: ObservableSet<PersistentNotification>
    private val onTransientNotificationListeners: MutableList<OnTransientNotificationListener>
    private val onImmediateNotificationListeners: MutableList<OnImmediateNotificationListener>

    // TODO fix circular reference
    @Inject
    private val i18n: I18n? = null

    init {

        persistentNotifications = synchronizedObservableSet(observableSet(TreeSet()))
        onTransientNotificationListeners = ArrayList()
        onImmediateNotificationListeners = ArrayList()
    }

    /**
     * Adds a [PersistentNotification] to be displayed.
     */

    fun addNotification(notification: PersistentNotification) {
        persistentNotifications.add(notification)
    }

    /**
     * Adds a [TransientNotification] to be displayed.
     */

    fun addNotification(notification: TransientNotification) {
        onTransientNotificationListeners.forEach { listener -> listener.onTransientNotification(notification) }
    }

    /**
     * Adds a [ImmediateNotification] to be displayed.
     */

    fun addNotification(notification: ImmediateNotification) {
        onImmediateNotificationListeners.forEach { listener -> listener.onImmediateNotification(notification) }
    }

    /**
     * Adds a listener to be notified about added/removed [PersistentNotification]s
     */

    fun addPersistentNotificationListener(listener: SetChangeListener<PersistentNotification>) {
        JavaFxUtil.addListener(persistentNotifications, listener)
    }

    /**
     * Adds a listener to be notified whenever a [TransientNotification] has been fired.
     */

    fun addTransientNotificationListener(listener: OnTransientNotificationListener) {
        onTransientNotificationListeners.add(listener)
    }


    fun getPersistentNotifications(): Set<PersistentNotification> {
        return Collections.unmodifiableSet(persistentNotifications)
    }


    fun removeNotification(notification: PersistentNotification) {
        persistentNotifications.remove(notification)
    }


    fun addImmediateNotificationListener(listener: OnImmediateNotificationListener) {
        onImmediateNotificationListeners.add(listener)
    }


    fun addPersistentErrorNotification(throwable: Throwable, messageKey: String, vararg args: Any) {
        addNotification(PersistentNotification(i18n!!.get(messageKey, *args), ERROR, listOf<Action>(ReportAction(i18n, reportingService, throwable))))
    }

    // TODO refactor code to use this method where applicable

    fun addImmediateErrorNotification(throwable: Throwable, messageKey: String, vararg args: Any) {
        addNotification(ImmediateNotification(i18n!!.get("errorTitle"), i18n.get(messageKey, *args), ERROR, throwable,
                Arrays.asList(DismissAction(i18n), ReportAction(i18n, reportingService, throwable))))
    }
}
