package com.faforever.client.map

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.ShowLadderMapsEvent
import com.faforever.client.map.event.MapUploadedEvent
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.query.SearchableProperties
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.vault.search.SearchController
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.google.common.collect.Iterators
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.jfoenix.controls.JFXDialog
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.DirectoryChooser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.io.File
import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO I'd like to avoid the additional "getMost*" methods and always use the map query function instead, however,
// this is currently not viable since Elide can't yet sort by relationship attributes. Once it supports that
// (see https://github.com/yahoo/elide/issues/353), this can be refactored.
class MapVaultController(private val mapService: MapService, private val i18n: I18n, private val eventBus: EventBus, private val preferencesService: PreferencesService,
                         private val uiService: UiService, private val notificationService: NotificationService, private val reportingService: ReportingService,
                         private val playerService: PlayerService) : AbstractViewController<Node>() {
    private val state: ObjectProperty<State>
    var searchResultGroup: Pane? = null
    var searchResultPane: Pane? = null
    var showroomGroup: Pane? = null
    var loadingLabel: Node? = null
    var newestPane: Pane? = null
    var mostPlayedPane: Pane? = null
    var mostLikedPane: Pane? = null
    var mapVaultRoot: StackPane? = null
    var scrollPane: ScrollPane? = null
    var backButton: Button? = null
    var searchController: SearchController? = null
    var moreButton: Button? = null
    var ladderPane: FlowPane? = null
    var ownedPane: FlowPane? = null
    var ownedMoreLabel: Label? = null
    var ownedMoreButton: Button? = null
    private var mapDetailController: MapDetailController? = null
    private var currentPage: Int = 0
    private var currentSupplier: Supplier<CompletableFuture<List<MapBean>>>? = null

    override val root: Node?
        get() = mapVaultRoot

    init {

        state = SimpleObjectProperty(State.UNINITIALIZED)
    }

    override fun initialize() {
        super.initialize()
        JavaFxUtil.fixScrollSpeed(scrollPane!!)

        loadingLabel!!.managedProperty().bind(loadingLabel!!.visibleProperty())
        showroomGroup!!.managedProperty().bind(showroomGroup!!.visibleProperty())
        searchResultGroup!!.managedProperty().bind(searchResultGroup!!.visibleProperty())
        backButton!!.managedProperty().bind(backButton!!.visibleProperty())
        moreButton!!.managedProperty().bind(moreButton!!.visibleProperty())

        mapDetailController = uiService.loadFxml("theme/vault/map/map_detail.fxml")
        val mapDetailRoot = mapDetailController!!.root
        mapVaultRoot!!.children.add(mapDetailRoot)
        AnchorPane.setTopAnchor(mapDetailRoot, 0.0)
        AnchorPane.setRightAnchor(mapDetailRoot, 0.0)
        AnchorPane.setBottomAnchor(mapDetailRoot, 0.0)
        AnchorPane.setLeftAnchor(mapDetailRoot, 0.0)
        mapDetailRoot!!.isVisible = false

        eventBus.register(this)

        searchController!!.setRootType(com.faforever.client.api.dto.Map::class.java)
        searchController!!.setSearchListener(Consumer<SearchConfig> { this.searchByQuery(it) })
        searchController!!.setSearchableProperties(SearchableProperties.MAP_PROPERTIES)
        searchController!!.setSortConfig(preferencesService.preferences!!.vaultPrefs.mapSortConfigProperty())
    }

    private fun searchByQuery(searchConfig: SearchConfig) {
        val newSearchConfig = SearchConfig(searchConfig.getSortConfig(), searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"")
        enterLoadingState()
        displayMapsFromSupplier({ mapService.findByQuery(newSearchConfig, ++currentPage, MAX_SEARCH_RESULTS) })
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (navigateEvent is ShowLadderMapsEvent) {
            showMoreLadderdMaps()
        } else if (state.get() == State.UNINITIALIZED) {
            displayShowroomMaps()
        }
    }

    private fun displayShowroomMaps() {
        enterLoadingState()
        mapService.getMostPlayedMaps(TOP_ELEMENT_COUNT, 1).thenAccept { maps -> replaceSearchResult(maps, mostPlayedPane) }
                .thenCompose { aVoid -> mapService.getHighestRatedMaps(TOP_ELEMENT_COUNT, 1) }.thenAccept { maps -> replaceSearchResult(maps, mostLikedPane) }
                .thenCompose { aVoid -> mapService.getNewestMaps(TOP_ELEMENT_COUNT, 1) }.thenAccept { maps -> replaceSearchResult(maps, newestPane) }
                .thenCompose { aVoid -> mapService.getLadderMaps(TOP_ELEMENT_COUNT, 1).thenAccept { maps -> replaceSearchResult(maps, ladderPane) } }
                .thenCompose { aVoid ->
                    val player = playerService.currentPlayer
                            .orElseThrow { IllegalStateException("Current player not set") }
                    mapService.getOwnedMaps(player.id, TOP_ELEMENT_COUNT, 1)
                }
                .thenAccept { mapBeans ->
                    if (mapBeans.isEmpty()) {
                        ownedPane!!.isVisible = false
                        ownedMoreButton!!.isVisible = false
                        ownedMoreLabel!!.isVisible = false
                    }
                    replaceSearchResult(mapBeans, ownedPane)
                }
                .thenRun { this.enterShowroomState() }
                .exceptionally { throwable ->
                    logger.warn("Could not populate maps", throwable)
                    null
                }
    }

    private fun replaceSearchResult(maps: List<MapBean>, pane: Pane?) {
        Platform.runLater { pane!!.children.clear() }
        appendSearchResult(maps, pane!!)
    }

    private fun enterLoadingState() {
        state.set(State.LOADING)
        showroomGroup!!.isVisible = false
        searchResultGroup!!.isVisible = false
        loadingLabel!!.isVisible = true
        backButton!!.isVisible = true
        moreButton!!.isVisible = false
    }

    private fun enterSearchResultState() {
        state.set(State.SEARCH_RESULT)
        showroomGroup!!.isVisible = false
        searchResultGroup!!.isVisible = true
        loadingLabel!!.isVisible = false
        backButton!!.isVisible = true
        moreButton!!.isVisible = searchResultPane!!.children.size % MAX_SEARCH_RESULTS == 0
    }

    private fun enterShowroomState() {
        state.set(State.SHOWROOM)
        showroomGroup!!.isVisible = true
        searchResultGroup!!.isVisible = false
        loadingLabel!!.isVisible = false
        backButton!!.isVisible = false
        moreButton!!.isVisible = false
    }

    private fun onShowMapDetail(map: MapBean) {
        mapDetailController!!.setMap(map)
        mapDetailController!!.root!!.isVisible = true
        mapDetailController!!.root!!.requestFocus()
    }

    fun onUploadMapButtonClicked() {
        Platform.runLater {
            val directoryChooser = DirectoryChooser()
            directoryChooser.initialDirectory = preferencesService.preferences!!.forgedAlliance.customMapsDirectory.toFile()
            directoryChooser.title = i18n.get("mapVault.upload.chooseDirectory")
            val result = directoryChooser.showDialog(root!!.scene.window)

            if (result == null) {
                return@Platform.runLater
            }
            openMapUploadWindow(result!!.toPath())
        }
    }

    private fun openMapUploadWindow(path: Path) {
        val mapUploadController = uiService.loadFxml<MapUploadController>("theme/vault/map/map_upload.fxml")
        mapUploadController.setMapPath(path)

        val root = mapUploadController.root
        val dialog = uiService.showInDialog(mapVaultRoot, root, i18n.get("mapVault.upload.title"))
        mapUploadController.setOnCancelButtonClickedListener { dialog.close() }
    }

    fun onRefreshButtonClicked() {
        mapService.evictCache()
        when (state.get()) {
            MapVaultController.State.SHOWROOM -> displayShowroomMaps()
            MapVaultController.State.SEARCH_RESULT -> {
                currentPage--
                currentSupplier!!.get()
                        .thenAccept(Consumer<List<MapBean>> { this.displayMaps(it) })
                        .exceptionally { throwable ->
                            notificationService.addNotification(ImmediateErrorNotification(
                                    i18n.get("errorTitle"), i18n.get("vault.mods.searchError"),
                                    throwable, i18n, reportingService
                            ))
                            enterShowroomState()
                            null
                        }
            }
        }// Do nothing
    }

    fun onBackButtonClicked() {
        enterShowroomState()
    }

    @Subscribe
    fun onMapUploaded(event: MapUploadedEvent) {
        onRefreshButtonClicked()
    }

    fun showMoreHighestRatedMaps() {
        enterLoadingState()
        displayMapsFromSupplier({ mapService.getHighestRatedMaps(LOAD_MORE_COUNT, ++currentPage) })
    }

    fun showMoreMostRecentMaps() {
        enterLoadingState()
        displayMapsFromSupplier({ mapService.getNewestMaps(LOAD_MORE_COUNT, ++currentPage) })
    }

    fun showMoreMostPlayedMaps() {
        enterLoadingState()
        displayMapsFromSupplier({ mapService.getMostPlayedMaps(LOAD_MORE_COUNT, ++currentPage) })
    }

    fun showMoreLadderdMaps() {
        enterLoadingState()
        displayMapsFromSupplier({ mapService.getLadderMaps(LOAD_MORE_COUNT, ++currentPage) })
    }

    fun showMoreOwnedMaps() {
        enterLoadingState()
        val currentPlayer = playerService.currentPlayer
                .orElseThrow { IllegalStateException("Current player was null") }
        displayMapsFromSupplier({ mapService.getOwnedMaps(currentPlayer.id, LOAD_MORE_COUNT, ++currentPage) })
    }

    private fun appendSearchResult(maps: List<MapBean>, pane: Pane) {
        JavaFxUtil.assertBackgroundThread()

        val children = pane.children
        val controllers = maps.parallelStream()
                .map { map ->
                    val controller = uiService.loadFxml<MapCardController>("theme/vault/map/map_card.fxml")
                    controller.setMap(map)
                    controller.setOnOpenDetailListener(Consumer<MapBean> { this.onShowMapDetail(it) })
                    controller
                }.collect<List<MapCardController>, Any>(Collectors.toList())

        Iterators.partition(controllers.iterator(), BATCH_SIZE).forEachRemaining { mapCardControllers ->
            Platform.runLater {
                for (mapCardController in mapCardControllers) {
                    children.add(mapCardController.root)
                }
            }
        }
    }

    private fun displayMapsFromSupplier(mapsSupplier: Supplier<CompletableFuture<List<MapBean>>>) {
        currentPage = 0
        this.currentSupplier = mapsSupplier
        mapsSupplier.get()
                .thenAccept(Consumer<List<MapBean>> { this.displayMaps(it) })
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"),
                            i18n.get("vault.maps.searchError"),
                            throwable,
                            i18n,
                            reportingService
                    ))
                    enterShowroomState()
                    null
                }
    }

    private fun displayMaps(maps: List<MapBean>) {
        Platform.runLater { searchResultPane!!.children.clear() }
        appendSearchResult(maps, searchResultPane!!)
        Platform.runLater { this.enterSearchResultState() }
    }

    fun onLoadMoreButtonClicked() {
        moreButton!!.isVisible = false
        loadingLabel!!.isVisible = true

        currentSupplier!!.get()
                .thenAccept { maps ->
                    appendSearchResult(maps, searchResultPane!!)
                    enterSearchResultState()
                }
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"), i18n.get("vault.maps.searchError"),
                            throwable, i18n, reportingService
                    ))
                    enterShowroomState()
                    null
                }
    }

    private enum class State {
        UNINITIALIZED, LOADING, SHOWROOM, SEARCH_RESULT
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val TOP_ELEMENT_COUNT = 7
        private val LOAD_MORE_COUNT = 100
        private val MAX_SEARCH_RESULTS = 100
        /**
         * How many mod cards should be badged into one UI thread runnable.
         */
        private val BATCH_SIZE = 10
    }
}
