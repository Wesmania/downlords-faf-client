package com.faforever.client.mod

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.util.TimeService
import com.faforever.client.vault.review.Review
import com.faforever.client.vault.review.ReviewService
import com.faforever.client.vault.review.ReviewsController
import com.faforever.commons.io.Bytes
import javafx.application.Platform
import javafx.collections.ListChangeListener
import javafx.collections.WeakListChangeListener
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ScrollPane
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.Optional

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class ModDetailController @Inject
constructor(private val modService: ModService, private val notificationService: NotificationService, private val i18n: I18n,
            private val reportingService: ReportingService, private val timeService: TimeService, private val reviewService: ReviewService,
            private val playerService: PlayerService) : Controller<Node> {

    var updatedLabel: Label? = null
    var sizeLabel: Label? = null
    var versionLabel: Label? = null
    var dependenciesTitle: Label? = null
    var dependenciesContainer: VBox? = null
    var progressLabel: Label? = null
    var scrollPane: ScrollPane? = null
    var uninstallButton: Button? = null
    var installButton: Button? = null
    var thumbnailImageView: ImageView? = null
    var nameLabel: Label? = null
    var authorLabel: Label? = null
    var progressBar: ProgressBar? = null
    var modDescriptionLabel: Label? = null
    override var root: Node? = null
    var reviewsController: ReviewsController? = null

    private var modVersion: ModVersion? = null
    private var installStatusChangeListener: ListChangeListener<ModVersion>? = null

    private val modSize: Long
        get() = modService.getModSize(modVersion)

    override fun initialize() {
        JavaFxUtil.fixScrollSpeed(scrollPane!!)
        uninstallButton!!.managedProperty().bind(uninstallButton!!.visibleProperty())
        installButton!!.managedProperty().bind(installButton!!.visibleProperty())
        progressBar!!.managedProperty().bind(progressBar!!.visibleProperty())
        progressBar!!.visibleProperty().bind(uninstallButton!!.visibleProperty().not().and(installButton!!.visibleProperty().not()))
        progressLabel!!.managedProperty().bind(progressLabel!!.visibleProperty())
        progressLabel!!.visibleProperty().bind(progressBar!!.visibleProperty())
        root!!.managedProperty().bind(root!!.visibleProperty())

        root!!.setOnKeyPressed { keyEvent ->
            if (keyEvent.code == KeyCode.ESCAPE) {
                onCloseButtonClicked()
            }
        }

        installStatusChangeListener = { change ->
            while (change.next()) {
                for (modVersion in change.getAddedSubList()) {
                    if (this.modVersion == modVersion) {
                        setInstalled(true)
                        return
                    }
                }
                for (modVersion in change.getRemoved()) {
                    if (this.modVersion == modVersion) {
                        setInstalled(false)
                        return
                    }
                }
            }
        }

        // TODO hidden until dependencies are available
        dependenciesTitle!!.isManaged = false
        dependenciesContainer!!.isManaged = false

        reviewsController!!.setCanWriteReview(false)
    }

    fun onCloseButtonClicked() {
        root!!.isVisible = false
    }

    private fun setInstalled(installed: Boolean) {
        installButton!!.isVisible = !installed
        uninstallButton!!.isVisible = installed
    }

    fun setModVersion(modVersion: ModVersion) {
        this.modVersion = modVersion
        thumbnailImageView!!.image = modService.loadThumbnail(modVersion)
        nameLabel!!.text = modVersion.displayName
        authorLabel!!.text = modVersion.uploader

        val modInstalled = modService.isModInstalled(modVersion.uid)
        installButton!!.isVisible = !modInstalled
        uninstallButton!!.isVisible = modInstalled

        modDescriptionLabel!!.text = modVersion.description
        JavaFxUtil.addListener(modService.installedModVersions, WeakListChangeListener(installStatusChangeListener!!))
        setInstalled(modService.isModInstalled(modVersion.uid))

        updatedLabel!!.text = timeService.asDate(modVersion.updateTime)
        sizeLabel!!.text = Bytes.formatSize(modSize, i18n.userSpecificLocale)
        versionLabel!!.text = modVersion.version.toString()

        val player = playerService.currentPlayer
                .orElseThrow { IllegalStateException("No current player is available") }

        reviewsController!!.setCanWriteReview(modService.isModInstalled(modVersion.uid))
        reviewsController!!.setOnSendReviewListener(Consumer<Review> { this.onSendReview(it) })
        reviewsController!!.setOnDeleteReviewListener(Consumer<Review> { this.onDeleteReview(it) })
        reviewsController!!.setReviews(modVersion.reviews)
        reviewsController!!.setOwnReview(modVersion.reviews.stream()
                .filter { review -> review.player.id == player.id }
                .findFirst())
    }

    private fun onDeleteReview(review: Review) {
        reviewService.deleteModVersionReview(review)
                .thenRun {
                    Platform.runLater {
                        modVersion!!.reviews.remove(review)
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
        reviewService.saveModVersionReview(review, modVersion!!.id)
                .thenRun {
                    if (isNew) {
                        modVersion!!.reviews.add(review)
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

        modService.downloadAndInstallMod(modVersion, progressBar!!.progressProperty(), progressLabel!!.textProperty())
                .thenRun { uninstallButton!!.isVisible = true }
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"),
                            i18n.get("modVault.installationFailed", modVersion!!.displayName, throwable.localizedMessage),
                            throwable, i18n, reportingService
                    ))
                    null
                }
    }

    fun onUninstallButtonClicked() {
        progressBar!!.progressProperty().unbind()
        progressBar!!.progress = -1.0
        uninstallButton!!.isVisible = false

        modService.uninstallMod(modVersion).exceptionally { throwable ->
            notificationService.addNotification(ImmediateErrorNotification(
                    i18n.get("errorTitle"),
                    i18n.get("modVault.couldNotDeleteMod", modVersion!!.displayName, throwable.localizedMessage),
                    throwable, i18n, reportingService
            ))
            null
        }
    }

    fun onDimmerClicked() {
        onCloseButtonClicked()
    }

    fun onContentPaneClicked(event: MouseEvent) {
        event.consume()
    }
}
