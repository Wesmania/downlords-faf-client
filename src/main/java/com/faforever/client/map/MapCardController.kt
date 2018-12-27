package com.faforever.client.map

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.util.IdenticonUtil
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
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.Optional
import java.util.function.Consumer

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class MapCardController @Inject
constructor(private val mapService: MapService, private val i18n: I18n) : Controller<Node> {

    var thumbnailImageView: ImageView? = null
    var nameLabel: Label? = null
    var mapTileRoot: Node? = null
    var authorLabel: Label? = null
    var starsController: StarsController? = null
    var numberOfReviewsLabel: Label? = null
    var numberOfPlaysLabel: Label? = null
    var sizeLabel: Label? = null
    var maxPlayersLabel: Label? = null

    private var map: MapBean? = null
    private var onOpenDetailListener: Consumer<MapBean>? = null
    private var installedMapsChangeListener: ListChangeListener<MapBean>? = null
    private val reviewsChangedListener: InvalidationListener
    private var jfxRippler: JFXRippler? = null

    override val root: Node?
        get() = jfxRippler

    init {
        reviewsChangedListener = { observable -> populateReviews() }
    }

    override fun initialize() {
        jfxRippler = JFXRippler(mapTileRoot)
        installedMapsChangeListener = { change ->
            while (change.next()) {
                for (mapBean in change.getAddedSubList()) {
                    if (map!!.id == mapBean.getId()) {
                        setInstalled(true)
                        return
                    }
                }
                for (mapBean in change.getRemoved()) {
                    if (map!!.id == mapBean.getId()) {
                        setInstalled(false)
                        return
                    }
                }
            }
        }
    }

    fun setMap(map: MapBean) {
        this.map = map
        val image: Image?
        if (map.largeThumbnailUrl != null) {
            image = mapService.loadPreview(map.largeThumbnailUrl, PreviewSize.SMALL)
        } else {
            image = IdenticonUtil.createIdenticon(map.id)
        }
        thumbnailImageView!!.image = image
        nameLabel!!.text = map.displayName
        authorLabel!!.text = Optional.ofNullable(map.author).orElse(i18n.get("map.unknownAuthor"))
        numberOfPlaysLabel!!.text = i18n.number(map.numberOfPlays)

        val size = map.size
        sizeLabel!!.text = i18n.get("mapPreview.size", size.widthInKm, size.heightInKm)
        maxPlayersLabel!!.text = i18n.number(map.players)

        val installedMaps = mapService.installedMaps
        JavaFxUtil.addListener(installedMaps, WeakListChangeListener(installedMapsChangeListener!!))

        val reviews = map.reviews
        JavaFxUtil.addListener(reviews, WeakInvalidationListener(reviewsChangedListener))
        reviewsChangedListener.invalidated(reviews)
    }

    private fun populateReviews() {
        val reviews = map!!.reviews
        Platform.runLater {
            numberOfReviewsLabel!!.text = i18n.number(reviews.size)
            starsController!!.value = reviews.stream().mapToInt(ToIntFunction<Review> { it.getScore() }).average().orElse(0.0).toFloat()
        }
    }

    private fun setInstalled(installed: Boolean) {
        // FIXME implement
    }

    fun setOnOpenDetailListener(onOpenDetailListener: Consumer<MapBean>) {
        this.onOpenDetailListener = onOpenDetailListener
    }

    fun onShowMapDetail() {
        onOpenDetailListener!!.accept(map)
    }
}
