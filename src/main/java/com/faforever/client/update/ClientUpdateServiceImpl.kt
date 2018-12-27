package com.faforever.client.update

import com.faforever.client.FafClientApplication
import com.faforever.client.fx.PlatformService
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.Action
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.task.TaskService
import com.google.common.annotations.VisibleForTesting
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import java.io.IOException
import java.lang.invoke.MethodHandles
import java.nio.file.Path

import com.faforever.client.notification.Severity.INFO
import com.faforever.client.notification.Severity.WARN
import com.faforever.commons.io.Bytes.formatSize
import java.util.Arrays.asList
import java.util.Collections.singletonList
import org.apache.commons.lang3.StringUtils.defaultString


@Lazy
@Service
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
class ClientUpdateServiceImpl(
        private val taskService: TaskService,
        private val notificationService: NotificationService,
        private val i18n: I18n,
        private val platformService: PlatformService,
        private val applicationContext: ApplicationContext
) : ClientUpdateService {

    @VisibleForTesting
    override var currentVersion: ComparableVersion
        internal set

    init {

        currentVersion = ComparableVersion(
                defaultString(Version.VERSION, DEVELOPMENT_VERSION_STRING)
        )
        logger.info("Current version: {}", currentVersion)
    }

    /**
     * Returns information about an available update. Returns `null` if no update is available.
     */
    override fun checkForUpdateInBackground() {
        val task = applicationContext.getBean(CheckForUpdateTask::class.java)
        task.setCurrentVersion(currentVersion)

        taskService.submitTask(task).future.thenAccept { updateInfo ->
            if (updateInfo == null) {
                return@taskService.submitTask(task).getFuture().thenAccept
            }
            notificationService.addNotification(PersistentNotification(
                    i18n.get("clientUpdateAvailable.notification", updateInfo!!.getName(), formatSize(updateInfo.getSize(), i18n.userSpecificLocale)),
                    INFO, asList(
                    Action(i18n.get("clientUpdateAvailable.downloadAndInstall")) { event -> downloadAndInstallInBackground(updateInfo) },
                    Action(i18n.get("clientUpdateAvailable.releaseNotes"), Action.Type.OK_STAY
                    ) { event -> platformService.showDocument(updateInfo.getReleaseNotesUrl().toExternalForm()) }))
            )
        }.exceptionally { throwable ->
            logger.warn("Client update check failed", throwable)
            null
        }
    }

    private fun install(binaryPath: Path) {
        // TODO probably need to make this executable on unix See #1026
        val command = binaryPath.toAbsolutePath().toString()
        try {
            logger.info("Starting installer at {}", command)
            ProcessBuilder(command).inheritIO().start()
        } catch (e: IOException) {
            logger.warn("Installation could not be started", e)
        }

    }

    private fun downloadAndInstallInBackground(updateInfo: UpdateInfo?) {
        val task = applicationContext.getBean(DownloadUpdateTask::class.java)
        task.setUpdateInfo(updateInfo)

        taskService.submitTask(task).future
                .thenAccept(Consumer<Path> { this.install(it) })
                .exceptionally { throwable ->
                    logger.warn("Error while downloading client update", throwable)
                    notificationService.addNotification(
                            PersistentNotification(i18n.get("clientUpdateDownloadFailed.notification"), WARN, listOf(Action(i18n.get("clientUpdateDownloadFailed.retry")) { event -> downloadAndInstallInBackground(updateInfo) }))
                    )
                    null
                }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val DEVELOPMENT_VERSION_STRING = "dev"
    }
}
