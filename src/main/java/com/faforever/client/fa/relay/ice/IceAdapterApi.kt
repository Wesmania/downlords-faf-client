package com.faforever.client.fa.relay.ice

/**
 * API functions provided by the ICE adapter process.
 */
internal interface IceAdapterApi {

    /** Gracefully shuts down the.  */
    fun quit()

    /** Tell the game to create the lobby and host game on Lobby-State.  */
    fun hostGame(mapName: String)

    /** Tell the game to create the Lobby, create a PeerRelay in answer mode and join the remote game.  */
    fun joinGame(remotePlayerLogin: String, remotePlayerId: Int)

    /** Create a PeerRelay and tell the game to connect to the remote peer with offer/answer mode.  */
    fun connectToPeer(remotePlayerLogin: String, remotePlayerId: Int, offer: Boolean)

    /** Destroy PeerRelay and tell the game to disconnect from the remote peer.  */
    fun disconnectFromPeer(remotePlayerId: Int)

    /**
     * Set the lobby mode the game will use. Supported values are "normal" for normal lobby and "auto" for automatch lobby
     * (aka ladder).
     */
    fun setLobbyInitMode(mode: String)

    /** Add the remote ICE message to the PeerRelay to establish a connection.  */
    fun iceMsg(remotePlayerId: Int, message: Any)

    /** Send an arbitrary message to the game.  */
    fun sendToGpgNet(header: String, chunks: List<Any>)

    /** Polls the current status of the faf-ice-adapter.  */
    fun status(): Map<String, Any>

    /**
     * ICE server array for use in webrtc. Must be called before joinGame/connectToPeer. See [https://developer.mozilla.org/en-US/docs/Web/API/RTCIceServer](https://developer.mozilla.org/en-US/docs/Web/API/RTCIceServer).
     */
    fun setIceServers(iceServers: List<Map<String, String>>): String
}
