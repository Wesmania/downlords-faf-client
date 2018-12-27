package com.faforever.client.map

import com.faforever.client.api.dto.ApiException
import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.map.event.MapUploadedEvent
import com.faforever.client.notification.Action
import com.faforever.client.notification.DismissAction
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.ReportAction
import com.faforever.client.reporting.ReportingService
import com.faforever.client.task.CompletableTask
import com.faforever.commons.map.PreviewGenerator
import com.google.common.eventbus.EventBus
import javafx.beans.binding.Bindings
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Node
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import lombok.SneakyThrows
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor

import com.faforever.client.notification.Severity.ERROR
import java.util.Arrays.asList

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class MapUploadController(private val mapService: MapService, private val threadPoolExecutor: ThreadPoolExecutor, private val notificationService: NotificationService, private val reportingService: ReportingService, private val i18n: I18n, private val eventBus: EventBus) : Controller<Node> {
    var rankedLabel: Label? = null
    var uploadTaskMessageLabel: Label? = null
    var uploadTaskTitleLabel: Label? = null
    var sizeLabel: Label? = null
    var playersLabel: Label? = null
    var parseProgressPane: Pane? = null
    var uploadProgressPane: Pane? = null
    var uploadCompletePane: Pane? = null
    var uploadProgressBar: ProgressBar? = null
    var mapInfoPane: Pane? = null
    var mapNameLabel: Label? = null
    var descriptionLabel: Label? = null
    var versionLabel: Label? = null
    var thumbnailImageView: ImageView? = null
    override var root: Region? = null
    var rankedCheckbox: CheckBox? = null
    private var mapPath: Path? = null
    private var mapInfo: MapBean? = null
    private var uploadMapTask: CompletableTask<Void>? = null
    private var cancelButtonClickedListener: Runnable? = null

    override fun initialize() {
        mapInfoPane!!.managedProperty().bind(mapInfoPane!!.visibleProperty())
        uploadProgressPane!!.managedProperty().bind(uploadProgressPane!!.visibleProperty())
        parseProgressPane!!.managedProperty().bind(parseProgressPane!!.visibleProperty())
        uploadCompletePane!!.managedProperty().bind(uploadCompletePane!!.visibleProperty())

        mapInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = false

        rankedLabel!!.labelFor = rankedCheckbox
    }

    fun setMapPath(mapPath: Path) {
        this.mapPath = mapPath
        enterParsingState()
        CompletableFuture.supplyAsync<MapBean>({ mapService.readMap(mapPath) }, threadPoolExecutor)
                .thenAccept(Consumer<MapBean> { this.setMapInfo(it) })
                .exceptionally { throwable ->
                    logger.warn("Map could not be read", throwable)
                    null
                }
    }

    private fun enterParsingState() {
        mapInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = true
        uploadCompletePane!!.isVisible = false
    }

    private fun setMapInfo(mapInfo: MapBean) {
        this.mapInfo = mapInfo
        enterMapInfoState()

        mapNameLabel!!.textProperty().bind(mapInfo.displayNameProperty())
        descriptionLabel!!.textProperty().bind(mapInfo.descriptionProperty())
        versionLabel!!.textProperty().bind(mapInfo.versionProperty().asString())
        sizeLabel!!.textProperty().bind(Bindings.createStringBinding(
                {
                    val mapSize = mapInfo.size
                    i18n.get("mapVault.upload.sizeFormat", mapSize.widthInKm, mapSize.heightInKm)
                }, mapInfo.sizeProperty())
        )
        playersLabel!!.textProperty().bind(Bindings.createStringBinding(
                { i18n.get("mapVault.upload.playersFormat", mapInfo.players) }, mapInfo.playersProperty())
        )

        thumbnailImageView!!.image = generatePreview(mapPath)
    }

    private fun enterMapInfoState() {
        mapInfoPane!!.isVisible = true
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = false
    }

    fun onCancelUploadClicked() {
        uploadMapTask!!.cancel(true)
        enterMapInfoState()
    }

    private fun onUploadFailed(throwable: Throwable) {
        enterMapInfoState()
        if (throwable is ApiException) {
            notificationService.addNotification(ImmediateNotification(
                    i18n.get("errorTitle"), i18n.get("mapVault.upload.failed", throwable.getLocalizedMessage()), ERROR,
                    asList(
                            Action(i18n.get("mapVault.upload.retry")) { event -> onUploadClicked() },
                            DismissAction(i18n)
                    )
            ))
        } else {
            notificationService.addNotification(ImmediateNotification(
                    i18n.get("errorTitle"), i18n.get("mapVault.upload.failed", throwable.localizedMessage), ERROR, throwable,
                    asList(
                            Action(i18n.get("mapVault.upload.retry")) { event -> onUploadClicked() },
                            ReportAction(i18n, reportingService, throwable),
                            DismissAction(i18n)
                    )
            ))
        }
    }

    fun onUploadClicked() {
        enterUploadingState()

        uploadProgressPane!!.isVisible = true
        uploadMapTask = mapService.uploadMap(mapPath, rankedCheckbox!!.isSelected)
        uploadTaskTitleLabel!!.textProperty().bind(uploadMapTask!!.titleProperty())
        uploadTaskMessageLabel!!.textProperty().bind(uploadMapTask!!.messageProperty())
        uploadProgressBar!!.progressProperty().bind(uploadMapTask!!.progressProperty())

        uploadMapTask!!.future
                .thenAccept { v -> eventBus.post(MapUploadedEvent(mapInfo)) }
                .thenAccept { aVoid -> enterUploadCompleteState() }
                .exceptionally { throwable ->
                    if (throwable !is CancellationException) {
                        onUploadFailed(throwable.cause)
                    }
                    null
                }
    }

    private fun enterUploadingState() {
        mapInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = true
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = false
    }

    private fun enterUploadCompleteState() {
        mapInfoPane!!.isVisible = false
        uploadProgressPane!!.isVisible = false
        parseProgressPane!!.isVisible = false
        uploadCompletePane!!.isVisible = true
    }

    fun onCancelClicked() {
        cancelButtonClickedListener!!.run()
    }

    internal fun setOnCancelButtonClickedListener(cancelButtonClickedListener: Runnable) {
        this.cancelButtonClickedListener = cancelButtonClickedListener
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        @SneakyThrows
        private fun generatePreview(mapPath: Path?): WritableImage {
            return SwingFXUtils.toFXImage(PreviewGenerator.generatePreview(mapPath!!, 256, 256), WritableImage(256, 256))
        }
    }
}
