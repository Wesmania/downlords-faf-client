package com.faforever.client.fa

import com.faforever.client.game.Faction
import com.faforever.client.preferences.ForgedAlliancePrefs
import org.springframework.util.StringUtils

import java.net.Inet4Address
import java.net.URI
import java.nio.file.Path
import java.util.ArrayList
import java.util.Arrays
import java.util.regex.Matcher
import java.util.regex.Pattern

class LaunchCommandBuilder {

    private var mean: Float? = null
    private var deviation: Float? = null
    private var country: String? = null
    private var clan: String? = null
    private var username: String? = null
    private var uid: Int? = null
    private var executable: Path? = null
    private var additionalArgs: List<String>? = null
    private var localGpgPort: Int? = null
    private var logFile: Path? = null
    private var replayFile: Path? = null
    private var replayId: Int? = null
    private var replayUri: URI? = null
    private var faction: Faction? = null
    private var executableDecorator: String? = null
    private var rehost: Boolean = false
    private var localReplayPort: Int? = null

    fun localGpgPort(localGpgPort: Int): LaunchCommandBuilder {
        this.localGpgPort = localGpgPort
        return this
    }

    fun localReplayPort(localReplayPort: Int): LaunchCommandBuilder {
        this.localReplayPort = localReplayPort
        return this
    }

    fun executable(executable: Path): LaunchCommandBuilder {
        this.executable = executable
        return this
    }

    fun uid(uid: Int?): LaunchCommandBuilder {
        this.uid = uid
        return this
    }

    fun mean(mean: Float?): LaunchCommandBuilder {
        this.mean = mean
        return this
    }

    fun deviation(deviation: Float?): LaunchCommandBuilder {
        this.deviation = deviation
        return this
    }

    fun country(country: String): LaunchCommandBuilder {
        this.country = country
        return this
    }

    fun clan(clan: String): LaunchCommandBuilder {
        this.clan = clan
        return this
    }

    fun username(username: String): LaunchCommandBuilder {
        this.username = username
        return this
    }

    fun logFile(logFile: Path): LaunchCommandBuilder {
        this.logFile = logFile
        return this
    }

    fun additionalArgs(additionalArgs: List<String>): LaunchCommandBuilder {
        this.additionalArgs = additionalArgs
        return this
    }

    fun replayId(replayId: Int?): LaunchCommandBuilder {
        this.replayId = replayId
        return this
    }

    fun replayFile(replayFile: Path): LaunchCommandBuilder {
        this.replayFile = replayFile
        return this
    }

    fun replayUri(replayUri: URI): LaunchCommandBuilder {
        this.replayUri = replayUri
        return this
    }

    fun faction(faction: Faction): LaunchCommandBuilder {
        this.faction = faction
        return this
    }

    fun rehost(rehost: Boolean): LaunchCommandBuilder {
        this.rehost = rehost
        return this
    }

    fun build(): List<String> {
        if (executableDecorator == null) {
            throw IllegalStateException("executableDecorator has not been set")
        }
        if (executable == null) {
            throw IllegalStateException("executable has not been set")
        }
        if (replayUri != null && uid != null) {
            throw IllegalStateException("uid and replayUri cannot be set at the same time")
        }
        if (uid != null && username == null) {
            throw IllegalStateException("username has not been set")
        }


        val command = ArrayList<String>()
        command.addAll(split(String.format(executableDecorator!!, executable!!.toAbsolutePath().toString())))
        command.addAll(Arrays.asList(
                "/init", ForgedAlliancePrefs.INIT_FILE_NAME,
                "/nobugreport"
        ))

        if (faction != null) {
            command.add(String.format("/%s", faction!!.string))
        }

        if (logFile != null) {
            command.add("/log")
            command.add(logFile!!.toAbsolutePath().toString())
        }

        val localIp = Inet4Address.getLoopbackAddress().hostAddress
        if (localGpgPort != null) {
            command.add("/gpgnet")
            command.add("$localIp:$localGpgPort")
        }

        if (mean != null) {
            command.add("/mean")
            command.add(mean.toString())
        }

        if (deviation != null) {
            command.add("/deviation")
            command.add(deviation.toString())
        }

        if (replayFile != null) {
            command.add("/replay")
            command.add(replayFile!!.toAbsolutePath().toString())
        } else if (replayUri != null) {
            command.add("/replay")
            command.add(replayUri!!.toASCIIString())
        }

        if (uid != null && localReplayPort != null) {
            command.add("/savereplay")
            command.add("gpgnet://$localIp:$localReplayPort/$uid/$username.SCFAreplay")
        }

        if (country != null) {
            command.add("/country")
            command.add(country)
        }

        if (!StringUtils.isEmpty(clan)) {
            command.add("/clan")
            command.add(clan)
        }

        if (replayId != null) {
            command.add("/replayid")
            command.add(replayId.toString())
        }

        if (rehost) {
            command.add("/rehost")
        }

        if (additionalArgs != null) {
            command.addAll(additionalArgs)
        }

        return command
    }

    fun executableDecorator(executableDecorator: String): LaunchCommandBuilder {
        this.executableDecorator = executableDecorator
        return this
    }

    companion object {

        private val QUOTED_STRING_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")\\s*")

        fun create(): LaunchCommandBuilder {
            return LaunchCommandBuilder()
        }

        private fun split(string: String): List<String> {
            val matcher = QUOTED_STRING_PATTERN.matcher(string)
            val result = ArrayList<String>()
            while (matcher.find()) {
                result.add(matcher.group(1).replace("\"", ""))
            }
            return result
        }
    }
}
