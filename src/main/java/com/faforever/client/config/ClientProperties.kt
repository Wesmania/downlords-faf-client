package com.faforever.client.config

import lombok.Data
import org.springframework.boot.context.properties.ConfigurationProperties

import java.time.Duration

@Data
@ConfigurationProperties(prefix = "faf-client", ignoreUnknownFields = false)
class ClientProperties {

    private val mainWindowTitle = "Downlord's FAF Client"
    private val news = News()
    private val forgedAlliance = ForgedAlliance()
    private val irc = Irc()
    private val server = Server()
    private val vault = Vault()
    private val replay = Replay()
    private val imgur = Imgur()
    private val trueSkill = TrueSkill()
    private val api = Api()
    private val unitDatabase = UnitDatabase()
    private val website = Website()
    private val translationProjectUrl: String? = null
    private val clientConfigUrl: String? = null
    private val clientConfigConnectTimeout = Duration.ofSeconds(30)

    @Data
    class News {
        /**
         * URL to fetch the RSS news feed from.
         */
        private val feedUrl: String? = null
    }

    @Data
    class ForgedAlliance {
        /**
         * Title of the Forged Alliance window. Required to find the window handle.
         */
        private val windowTitle = "Forged Alliance"

        /**
         * URL to download the ForgedAlliance.exe from.
         */
        private val exeUrl: String? = null
    }

    @Data
    class Irc {
        private val host: String? = null
        private val port = 8167
        /**
         * Channel to join by default.
         *
         */
        @Deprecated("shouldn't be known by the client but sent from the server.")
        private val defaultChannel = "#aeolus"
        private val reconnectDelay = Duration.ofSeconds(5).toMillis().toInt()
    }

    @Data
    class Server {
        private val host: String? = null
        private val port = 8001
    }

    @Data
    class Vault {
        private val baseUrl: String? = null
        private val mapDownloadUrlFormat: String? = null
        private val mapPreviewUrlFormat: String? = null
        private val replayDownloadUrlFormat: String? = null
        private val modDownloadUrlFormat: String? = null
    }

    @Data
    class Replay {
        private val remoteHost: String? = null
        private val remotePort = 15000
        private val replayFileFormat = "%d-%s.fafreplay"
        private val replayFileGlob = "*.fafreplay"
        // TODO this should acutally be reported by the server
        private val watchDelaySeconds = 300
    }

    @Data
    class Imgur {
        private val upload = Upload()

        @Data
        class Upload {
            private val baseUrl = "https://api.imgur.com/3/image"
            private val clientId: String? = null
            private val maxSize = 2097152
        }
    }


    @Data
    @Deprecated("load from server")
    class TrueSkill {
        private val initialStandardDeviation: Int = 0
        private val initialMean: Int = 0
        private val beta: Int = 0
        private val dynamicFactor: Float = 0.toFloat()
        private val drawProbability: Float = 0.toFloat()
    }

    @Data
    class Website {
        private val baseUrl: String? = null
        private val forgotPasswordUrl: String? = null
        private val createAccountUrl: String? = null
    }

    @Data
    class Api {
        private val baseUrl: String? = null
        private val clientId: String? = null
        private val clientSecret: String? = null
        private val maxPageSize = 10000
    }

    @Data
    class UnitDatabase {
        private val spookiesUrl: String? = null
        private val rackOversUrl: String? = null
    }
}
