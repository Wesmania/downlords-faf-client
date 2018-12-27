package com.faforever.client.mod

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.util.TimeService
import com.faforever.client.vault.review.Review
import com.faforever.client.vault.review.StarsController
import com.jfoenix.controls.JFXRippler
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.function.Consumer

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ModCardController @Inject
constructor(private val modService: ModService, private val timeService: TimeService, private val i18n: I18n) : Controller<Node> {
    var thumbnailImageView: ImageView? = null
    var nameLabel: Label? = null
    var authorLabel: Label? = null
    var modTileRoot: Node? = null
    var createdLabel: Label? = null
    var numberOfReviewsLabel: Label? = null
    var typeLabel: Label? = null
    private var modVersion: ModVersion? = null
    private var onOpenDetailListener: Consumer<ModVersion>? = null
    private var installStatusChangeListener: ListChangeListener<ModVersion>? = null
    var starsController: StarsController? = null
    private val reviewsChangedListener: InvalidationListener
    private var jfxRippler: JFXRippler? = null

    override val root: Node?
        get() = jfxRippler


    init {
        reviewsChangedListener = { observable -> populateReviews() }
    }

    private fun populateReviews() {
        val reviews = modVersion!!.reviews
        Platform.runLater {
            numberOfReviewsLabel!!.text = i18n.number(reviews.size)
            starsController!!.value = reviews.stream().mapToInt(ToIntFunction<Review> { it.getScore() }).average().orElse(0.0).toFloat()
        }
    }

    override fun initialize() {
        jfxRippler = JFXRippler(modTileRoot)
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
    }

    private fun setInstalled(installed: Boolean) {
        //TODO:IMPLEMENT ISSUE #670
    }

    fun setModVersion(modVersion: ModVersion) {
        this.modVersion = modVersion
        thumbnailImageView!!.image = modService.loadThumbnail(modVersion)
        nameLabel!!.text = modVersion.displayName
        authorLabel!!.text = modVersion.uploader
        createdLabel!!.text = timeService.asDate(modVersion.createTime)
        typeLabel!!.text = if (modVersion.modType != null) i18n.get(modVersion.modType.getI18nKey()) else ""

        val installedModVersions = modService.installedModVersions
        JavaFxUtil.addListener(installedModVersions, WeakListChangeListener(installStatusChangeListener!!))

        val reviews = modVersion.reviews
        JavaFxUtil.addListener(reviews, WeakInvalidationListener(reviewsChangedListener))
        reviewsChangedListener.invalidated(reviews)
    }

    fun setOnOpenDetailListener(onOpenDetailListener: Consumer<ModVersion>) {
        this.onOpenDetailListener = onOpenDetailListener
    }

    fun onShowModDetail() {
        onOpenDetailListener!!.accept(modVersion)
    }
}
