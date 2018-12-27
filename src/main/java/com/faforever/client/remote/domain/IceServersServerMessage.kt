package com.faforever.client.remote.domain

import com.google.gson.annotations.SerializedName
import lombok.Data
import lombok.EqualsAndHashCode

/**
 * Message sent from the server to the client containing a list of ICE servers to use.
 */
@Data
@EqualsAndHashCode(callSuper = true)
class IceServersServerMessage : FafServerMessage() {

    @SerializedName("ice_servers")
    private val iceServers: List<IceServer>? = null

    @Data
    class IceServer {
        private val url: String? = null
        private val credential: String? = null
        private val credentialType: String? = null
        private val username: String? = null
    }
}
