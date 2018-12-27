package com.faforever.client.update

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.task.CompletableTask
import com.faforever.client.update.ClientConfiguration.ReleaseInfo
import com.google.common.annotations.VisibleForTesting
import lombok.SneakyThrows
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.io.IOException
import java.lang.invoke.MethodHandles
import java.net.URL
import java.util.regex.Pattern

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class CheckForUpdateTask(private val i18n: I18n, private val preferencesService: PreferencesService) : CompletableTask<UpdateInfo>(CompletableTask.Priority.LOW) {

    private var currentVersion: ComparableVersion? = null

    @VisibleForTesting
    internal var fileSizeReader = { url ->
        url
                .openConnection()
                .getContentLength()
    }

    @Throws(Exception::class)
    public override fun call(): UpdateInfo? {
        updateTitle(i18n.get("clientUpdateCheckTask.title"))
        logger.info("Checking for client update")

        // .get() because this task runs asynchronously already
        val clientConfiguration = preferencesService.remotePreferences.get()

        val latestRelease = clientConfiguration.getLatestRelease()
        val version = latestRelease.getVersion()

        logger.info("Current version is {}, newest version is {}", currentVersion, version)

        if (!SEMVER_PATTERN.matcher(version).matches()) {
            return null
        }

        // Strip the "v" prefix
        val latestVersion = ComparableVersion(version.substring(1))

        if (latestVersion.compareTo(currentVersion!!) < 1) {
            return null
        }

        val downloadUrl: URL?
        if (org.bridj.Platform.isWindows()) {
            downloadUrl = latestRelease.getWindowsUrl()
        } else if (org.bridj.Platform.isLinux()) {
            downloadUrl = latestRelease.getLinuxUrl()
        } else if (org.bridj.Platform.isMacOSX()) {
            downloadUrl = latestRelease.getMacUrl()
        } else {
            return null
        }
        if (downloadUrl == null) {
            return null
        }

        val fileSize = getFileSize(downloadUrl)

        return UpdateInfo(
                latestVersion.canonical,
                downloadUrl!!.file.substring(downloadUrl.file.lastIndexOf('/') + 1),
                downloadUrl,
                fileSize,
                latestRelease.getReleaseNotesUrl()
        )
    }

    @SneakyThrows
    private fun getFileSize(downloadUrl: URL): Int {
        return fileSizeReader.read(downloadUrl)!!
    }

    fun setCurrentVersion(currentVersion: ComparableVersion) {
        this.currentVersion = currentVersion
    }

    // TODO make this available as a bean and use it in MapService as well
    @VisibleForTesting
    internal interface FileSizeReader {
        @Throws(IOException::class)
        fun read(url: URL): Int?
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val SEMVER_PATTERN = Pattern.compile("v\\d+(\\.\\d+)*[^.]*")
    }
}
