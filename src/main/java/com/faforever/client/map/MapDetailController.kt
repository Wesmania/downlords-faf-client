package com.faforever.client.map

import com.faforever.client.fa.FaStrings
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.HostGameEvent
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.util.IdenticonUtil
import com.faforever.client.util.TimeService
import com.faforever.client.vault.review.Review
import com.faforever.client.vault.review.ReviewService
import com.faforever.client.vault.review.ReviewsController
import com.faforever.commons.io.Bytes
import com.google.common.base.Strings
import com.google.common.eventbus.EventBus
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ScrollPane
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.RowConstraints
import javafx.scene.layout.VBox
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.time.LocalDateTime
import java.util.Optional

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class MapDetailController @Inject
constructor(private val mapService: MapService, private val notificationService: NotificationService, private val i18n: I18n,
            private val reportingService: ReportingService, private val timeService: TimeService, private val playerService: PlayerService,
            private val reviewService: ReviewService, private val eventBus: EventBus) : Controller<Node> {

    var progressLabel: Label? = null
    var uninstallButton: Button? = null
    var installButton: Button? = null
    var thumbnailImageView: ImageView? = null
    var nameLabel: Label? = null
    var authorLabel: Label? = null
    var progressBar: ProgressBar? = null
    var mapDescriptionLabel: Label? = null
    override var root: Node? = null
    var scrollPane: ScrollPane? = null
    var dimensionsLabel: Label? = null
    var maxPlayersLabel: Label? = null
    var dateLabel: Label? = null
    var isHiddenLabel: Label? = null
    var isRankedLabel: Label? = null
    var reviewsController: ReviewsController? = null
    var loadingContainer: VBox? = null
    var hideRow: RowConstraints? = null
    var hideButton: Button? = null
    var unrankButton: Button? = null
    var hideBox: HBox? = null

    private var map: MapBean? = null
    private var installStatusChangeListener: ListChangeListener<MapBean>? = null

    override fun initialize() {
        JavaFxUtil.fixScrollSpeed(scrollPane!!)
        uninstallButton!!.managedProperty().bind(uninstallButton!!.visibleProperty())
        installButton!!.managedProperty().bind(installButton!!.visibleProperty())
        progressBar!!.managedProperty().bind(progressBar!!.visibleProperty())
        progressBar!!.visibleProperty().bind(uninstallButton!!.visibleProperty().not().and(installButton!!.visibleProperty().not()))
        progressLabel!!.managedProperty().bind(progressLabel!!.visibleProperty())
        progressLabel!!.visibleProperty().bind(progressBar!!.visibleProperty())
        loadingContainer!!.visibleProperty().bind(progressBar!!.visibleProperty())
        hideButton!!.managedProperty().bind(hideButton!!.visibleProperty())
        unrankButton!!.managedProperty().bind(unrankButton!!.visibleProperty())
        hideBox!!.managedProperty().bind(hideBox!!.visibleProperty())

        reviewsController!!.setCanWriteReview(false)

        root!!.setOnKeyPressed { keyEvent ->
            if (keyEvent.code == KeyCode.ESCAPE) {
                onCloseButtonClicked()
            }
        }

        installStatusChangeListener = { change ->
            while (change.next()) {
                for (mapBean in change.getAddedSubList()) {
                    if (map!!.folderName.equals(mapBean.getFolderName(), ignoreCase = true)) {
                        setInstalled(true)
                        return
                    }
                }
                for (mapBean in change.getRemoved()) {
                    if (map!!.folderName == mapBean.getFolderName()) {
                        setInstalled(false)
                        return
                    }
                }
            }
        }
    }

    private fun renewAuthorControls() {
        val currentPlayer = playerService.currentPlayer
        val player = currentPlayer.orElseThrow { IllegalStateException("Player must be set in vault") }
        val viewerIsAuthor = map!!.author != null && player.username.toString() == map!!.author
        unrankButton!!.isVisible = viewerIsAuthor && map!!.isRanked
        hideButton!!.isVisible = viewerIsAuthor && !map!!.isHidden
        isHiddenLabel!!.text = if (map!!.isHidden) i18n.get("yes") else i18n.get("no")
        isRankedLabel!!.text = if (map!!.isRanked) i18n.get("yes") else i18n.get("no")
        removeHideRow(!viewerIsAuthor)
    }

    private fun removeHideRow(hide: Boolean) {
        hideBox!!.isVisible = !hide
        hideRow!!.maxHeight = if (hide) 0.0 else Control.USE_COMPUTED_SIZE
        hideRow!!.prefHeight = if (hide) 0.0 else Control.USE_COMPUTED_SIZE
        hideRow!!.minHeight = if (hide) 0.0 else Control.USE_COMPUTED_SIZE
    }

    fun onCloseButtonClicked() {
        root!!.isVisible = false
    }

    private fun setInstalled(installed: Boolean) {
        installButton!!.isVisible = !installed
        uninstallButton!!.isVisible = installed
    }

    fun setMap(map: MapBean) {
        this.map = map
        if (map.largeThumbnailUrl != null) {
            thumbnailImageView!!.image = mapService.loadPreview(map, PreviewSize.LARGE)
        } else {
            thumbnailImageView!!.image = IdenticonUtil.createIdenticon(map.id)
        }
        renewAuthorControls()
        nameLabel!!.text = map.displayName
        authorLabel!!.text = Optional.ofNullable(map.author).orElse(i18n.get("map.unknownAuthor"))
        maxPlayersLabel!!.text = i18n.number(map.players)

        val mapSize = map.size
        dimensionsLabel!!.text = i18n.get("mapPreview.size", mapSize.widthInKm, mapSize.heightInKm)

        val createTime = map.createTime
        dateLabel!!.text = timeService.asDate(createTime)

        val mapInstalled = mapService.isInstalled(map.folderName)
        installButton!!.isVisible = !mapInstalled

        val player = playerService.currentPlayer.orElseThrow { IllegalStateException("No user is logged in") }

        reviewsController!!.setCanWriteReview(false)
        mapService.hasPlayedMap(player.id, map.id)
                .thenAccept { hasPlayed -> reviewsController!!.setCanWriteReview(hasPlayed!!) }

        reviewsController!!.setOnSendReviewListener(Consumer<Review> { this.onSendReview(it) })
        reviewsController!!.setOnDeleteReviewListener(Consumer<Review> { this.onDeleteReview(it) })
        reviewsController!!.setReviews(map.reviews)
        reviewsController!!.setOwnReview(map.reviews.stream()
                .filter { review -> review.player.id == player.id }
                .findFirst())

        mapService.getFileSize(map.downloadUrl)
                .thenAccept { mapFileSize ->
                    Platform.runLater {
                        if (mapFileSize > -1) {
                            installButton!!.text = i18n.get("mapVault.installButtonFormat", Bytes.formatSize(mapFileSize!!.toLong(), i18n.userSpecificLocale))
                            installButton!!.isDisable = false
                        } else {
                            installButton!!.text = i18n.get("notAvailable")
                            installButton!!.isDisable = true
                        }
                    }
                }
        uninstallButton!!.isVisible = mapInstalled

        mapDescriptionLabel!!.text = Optional.ofNullable(map.description)
                .map<String>(Function<String, String> { Strings.emptyToNull(it) })
                .map(Function<String, String> { obj, description -> obj.removeLocalizationTag(description) })
                .orElseGet({ i18n.get("map.noDescriptionAvailable") })

        val installedMaps = mapService.installedMaps
        JavaFxUtil.addListener(installedMaps, WeakListChangeListener(installStatusChangeListener!!))

        setInstalled(mapService.isInstalled(map.folderName))
    }

    private fun onDeleteReview(review: Review) {
        reviewService.deleteMapVersionReview(review)
                .thenRun {
                    Platform.runLater {
                        map!!.reviews.remove(review)
                        reviewsController!!.setOwnReview(Optional.empty())
                    }
                }
                // TODO display error to user
                .exceptionally { throwable ->
                    log.warn("Review could not be deleted", throwable)
                    null
                }
    }

    private fun onSendReview(review: Review) {
        val isNew = review.id == null
        val player = playerService.currentPlayer
                .orElseThrow { IllegalStateException("No current player is available") }
        review.player = player
        reviewService.saveMapVersionReview(review, map!!.id)
                .thenRun {
                    if (isNew) {
                        map!!.reviews.add(review)
                    }
                    reviewsController!!.setOwnReview(Optional.of(review))
                }
                // TODO display error to user
                .exceptionally { throwable ->
                    log.warn("Review could not be saved", throwable)
                    null
                }
    }

    fun onInstallButtonClicked() {
        installButton!!.isVisible = false

        mapService.downloadAndInstallMap(map, progressBar!!.progressProperty(), progressLabel!!.textProperty())
                .thenRun { setInstalled(true) }
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"),
                            i18n.get("mapVault.installationFailed", map!!.displayName, throwable.localizedMessage),
                            throwable, i18n, reportingService
                    ))
                    setInstalled(false)
                    null
                }
    }

    fun onUninstallButtonClicked() {
        progressBar!!.progressProperty().unbind()
        progressBar!!.progress = -1.0
        uninstallButton!!.isVisible = false

        mapService.uninstallMap(map)
                .thenRun { setInstalled(false) }
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"),
                            i18n.get("mapVault.couldNotDeleteMap", map!!.displayName, throwable.localizedMessage),
                            throwable, i18n, reportingService
                    ))
                    setInstalled(true)
                    null
                }
    }

    fun onDimmerClicked() {
        onCloseButtonClicked()
    }

    fun onContentPaneClicked(event: MouseEvent) {
        event.consume()
    }

    fun onCreateGameButtonClicked() {
        eventBus.post(HostGameEvent(map!!.folderName))
    }

    fun hideMap() {
        mapService.hideMapVersion(map).thenAccept { aVoid ->
            Platform.runLater {
                map!!.isHidden = true
                renewAuthorControls()
            }
        }.exceptionally { throwable ->
            notificationService.addImmediateErrorNotification(throwable, "map.couldNotHide")
            log.error("Could not hide map", throwable)
            null
        }
    }

    fun unrankMap() {
        mapService.unrankMapVersion(map)
                .thenAccept { aVoid ->
                    Platform.runLater {
                        map!!.isRanked = false
                        renewAuthorControls()
                    }
                }
                .exceptionally { throwable ->
                    notificationService.addImmediateErrorNotification(throwable, "map.couldNotUnrank")
                    log.error("Could not unrank map", throwable)
                    null
                }
    }
}
