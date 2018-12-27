package com.faforever.client.fa.relay.ice

import com.faforever.client.fa.relay.GpgGameMessage
import com.faforever.client.fa.relay.ice.event.GpgGameMessageEvent
import com.faforever.client.fa.relay.ice.event.IceAdapterStateChanged
import com.faforever.client.remote.FafService
import com.google.common.eventbus.EventBus
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

/**
 * Dispatches all methods that the ICE adapter can call on its client.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class IceAdapterCallbacks @Inject
constructor(private val eventBus: EventBus, private val fafService: FafService) {

    fun onConnectionStateChanged(newState: String) {
        log.debug("ICE adapter connection state changed to: {}", newState)
        eventBus.post(IceAdapterStateChanged(newState))
    }

    fun onGpgNetMessageReceived(header: String, chunks: List<Any>) {
        log.debug("Message from game: '{}' '{}'", header, chunks)
        eventBus.post(GpgGameMessageEvent(GpgGameMessage(header, chunks)))
    }

    fun onIceMsg(localPlayerId: Long, remotePlayerId: Long, message: Any) {
        log.debug("ICE message for connection '{}/{}': {}", localPlayerId, remotePlayerId, message)
        fafService.sendIceMessage(remotePlayerId.toInt(), message)
    }

    fun onIceConnectionStateChanged(localPlayerId: Long, remotePlayerId: Long, state: String) {
        log.debug("ICE connection state for peer '{}' changed to: {}", remotePlayerId, state)
    }

    fun onConnected(localPlayerId: Long, remotePlayerId: Long, connected: Boolean) {
        if (connected) {
            log.debug("Connection between '{}' and '{}' has been established", localPlayerId, remotePlayerId)
        } else {
            log.debug("Connection between '{}' and '{}' has been lost", localPlayerId, remotePlayerId)
        }
    }
}
