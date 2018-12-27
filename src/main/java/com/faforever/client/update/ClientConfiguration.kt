package com.faforever.client.update

import lombok.Data

import java.net.URL

@Data
// TODO since this class contains both, update info and configuration, the package 'update' doesn't really fit.
class ClientConfiguration {
    internal var latestRelease: ReleaseInfo? = null
    internal var endpoints: List<Endpoints>? = null

    @Data
    class Endpoints {
        internal var name: String? = null
        internal var lobby: SocketEndpoint? = null
        internal var irc: SocketEndpoint? = null
        internal var liveReplay: SocketEndpoint? = null
        internal var api: UrlEndpoint? = null
    }

    @Data
    class SocketEndpoint {
        internal var host: String? = null
        internal var port: Int = 0
    }

    @Data
    class UrlEndpoint {
        internal var url: String? = null
    }

    @Data
    inner class ReleaseInfo {
        internal var version: String? = null
        internal var windowsUrl: URL? = null
        internal var linuxUrl: URL? = null
        internal var macUrl: URL? = null
        internal var mandatory: Boolean = false
        internal var message: String? = null
        internal var releaseNotesUrl: URL? = null
    }
}