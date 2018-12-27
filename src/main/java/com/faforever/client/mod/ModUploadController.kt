package com.faforever.client.mod

import com.faforever.client.api.dto.ApiException
import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.mod.event.ModUploadedEvent
import com.faforever.client.notification.Action
import com.faforever.client.notification.DismissAction
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.ReportAction
import com.faforever.client.reporting.ReportingService
import com.faforever.client.task.CompletableTask
import com.google.common.eventbus.EventBus
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor

import com.faforever.client.notification.Severity.ERROR
import java.util.Arrays.asList

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ModUploadController @Inject
constructor(private val modService: ModService, private val threadPoolExecutor: ThreadPoolExecutor, private val notificationService: NotificationService, private val reportingService: ReportingService, private val i18n: I18n, private val eventBus: EventBus) : Controller<Node> {
    var uploadTaskMessageLabel: Label? = null
    var uploadTaskTitleLabel: Label? = null
    var parseProgressPane: Pane? = null
    var uploadProgressPane: Pane? = null
    var uploadCompletePane: Pane? = null
    var uploadProgressBar: ProgressBar? = null
    var modInfoPane: Pane? = null
    var modNameLabel: Label? = null
    var descriptionLabel: Label? = null
    var versionLabel: Label? = null
    var uidLabel: Label? = null
    var thumbnailImageView: ImageView? = null
    override var root: Region? = null
    private var modPath: Path? = null
    private var modUploadTask: CompletableTask<Void>? = null
    private var modVersionInfo: ModVersion? = null
    private var cancelButtonClickedListener: Runnable? = null

    override fun initialize() {
        modInfoPane!!.managedProperty().bind(modInfoPane!!.visibleProperty())
        uploadProgressPane!!.managedProperty().bind(uploadProgressPane!!.visibleProperty())
        parseProgressPane!!.managedProperty().bind(parseProgressPane!!.visibleProperty())
        uploadCompletePane!!.managedProperty().bind(uploadCompletePane!!.visibleProperty())

        modInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = false
    }

    fun setModPath(modPath: Path) {
        this.modPath = modPath
        enterParsingState()
        CompletableFuture.supplyAsync<ModVersion>({ modService.extractModInfo(modPath) }, threadPoolExecutor)
                .thenAccept(Consumer<ModVersion> { this.setModVersionInfo(it) })
                .exceptionally { throwable ->
                    logger.warn("ModVersion could not be read", throwable)
                    null
                }
    }

    private fun enterParsingState() {
        modInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = true
        uploadCompletePane!!.isVisible = false
    }

    private fun setModVersionInfo(modVersion: ModVersion) {
        this.modVersionInfo = modVersion
        Platform.runLater {
            enterModInfoState()
            modNameLabel!!.textProperty().bind(modVersion.displayNameProperty())
            descriptionLabel!!.textProperty().bind(modVersion.descriptionProperty())
            versionLabel!!.textProperty().bind(modVersion.versionProperty().asString())
            uidLabel!!.textProperty().bind(modVersion.idProperty())
            thumbnailImageView!!.imageProperty().bind(
                    Bindings.createObjectBinding<Image>({ modService.loadThumbnail(modVersion) }, modVersion.idProperty(), modVersion.imagePathProperty())
            )
        }
    }

    private fun enterModInfoState() {
        modInfoPane!!.isVisible = true
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = false
    }

    fun onCancelUploadClicked() {
        modUploadTask!!.cancel(true)
        enterModInfoState()
    }

    private fun onUploadFailed(throwable: Throwable) {
        enterModInfoState()
        if (throwable is ApiException) {
            notificationService.addNotification(ImmediateNotification(
                    i18n.get("errorTitle"), i18n.get("modVault.upload.failed", throwable.getLocalizedMessage()), ERROR,
                    asList(
                            Action(i18n.get("modVault.upload.retry")) { event -> onUploadClicked() },
                            DismissAction(i18n)
                    )
            ))
        } else {
            notificationService.addNotification(ImmediateNotification(
                    i18n.get("errorTitle"), i18n.get("modVault.upload.failed", throwable.localizedMessage), ERROR, throwable,
                    asList(
                            Action(i18n.get("modVault.upload.retry")) { event -> onUploadClicked() },
                            ReportAction(i18n, reportingService, throwable),
                            DismissAction(i18n)
                    )
            ))
        }
    }

    fun onUploadClicked() {
        enterUploadingState()

        uploadProgressPane!!.isVisible = true
        modUploadTask = modService.uploadMod(modPath)
        uploadTaskTitleLabel!!.textProperty().bind(modUploadTask!!.titleProperty())
        uploadTaskMessageLabel!!.textProperty().bind(modUploadTask!!.messageProperty())
        uploadProgressBar!!.progressProperty().bind(modUploadTask!!.progressProperty())

        modUploadTask!!.future
                .thenAccept { v -> eventBus.post(ModUploadedEvent(modVersionInfo)) }
                .thenAccept { aVoid -> enterUploadCompleteState() }
                .exceptionally { throwable ->
                    if (throwable !is CancellationException) {
                        onUploadFailed(throwable.cause)
                    }
                    null
                }
    }

    private fun enterUploadingState() {
        modInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = true
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = false
    }

    private fun enterUploadCompleteState() {
        modInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = true
    }

    fun onCancelClicked() {
        cancelButtonClickedListener!!.run()
    }

    fun setOnCancelButtonClickedListener(cancelButtonClickedListener: Runnable) {
        this.cancelButtonClickedListener = cancelButtonClickedListener
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
