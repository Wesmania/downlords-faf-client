package com.faforever.client.mod

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.mod.event.ModUploadedEvent
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.query.SearchableProperties
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.vault.search.SearchController
import com.faforever.client.vault.search.SearchController.SearchConfig
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Iterators
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.jfoenix.controls.JFXDialog
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.DirectoryChooser
import lombok.extern.slf4j.Slf4j
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
@Slf4j
class ModVaultController(private val modService: ModService, private val i18n: I18n, private val eventBus: EventBus, private val preferencesService: PreferencesService,
                         private val uiService: UiService, private val notificationService: NotificationService, private val reportingService: ReportingService) : AbstractViewController<Node>() {

    var searchResultGroup: Pane? = null
    var searchResultPane: Pane? = null
    var showroomGroup: Pane? = null
    var loadingLabel: Node? = null
    var highestRatedUiPane: Pane? = null
    var newestPane: Pane? = null
    var highestRatedPane: Pane? = null
    var modVaultRoot: StackPane? = null
    var scrollPane: ScrollPane? = null
    var backButton: Button? = null
    var searchController: SearchController? = null
    var moreButton: Button? = null

    private var initialized: Boolean = false
    private var modDetailController: ModDetailController? = null
    private var state: ModVaultController.State? = null
    private var currentPage: Int = 0
    private var currentSupplier: Supplier<CompletableFuture<List<ModVersion>>>? = null

    override val root: Node?
        get() = modVaultRoot

    override fun initialize() {
        super.initialize()
        JavaFxUtil.fixScrollSpeed(scrollPane!!)

        loadingLabel!!.managedProperty().bind(loadingLabel!!.visibleProperty())
        showroomGroup!!.managedProperty().bind(showroomGroup!!.visibleProperty())
        searchResultGroup!!.managedProperty().bind(searchResultGroup!!.visibleProperty())
        backButton!!.managedProperty().bind(backButton!!.visibleProperty())
        moreButton!!.managedProperty().bind(moreButton!!.visibleProperty())

        modDetailController = uiService.loadFxml("theme/vault/mod/mod_detail.fxml")
        val modDetailRoot = modDetailController!!.root
        modVaultRoot!!.children.add(modDetailRoot)
        AnchorPane.setTopAnchor(modDetailRoot, 0.0)
        AnchorPane.setRightAnchor(modDetailRoot, 0.0)
        AnchorPane.setBottomAnchor(modDetailRoot, 0.0)
        AnchorPane.setLeftAnchor(modDetailRoot, 0.0)
        modDetailRoot!!.isVisible = false

        eventBus.register(this)

        searchController!!.setRootType(com.faforever.client.api.dto.Mod::class.java)
        searchController!!.setSearchListener(Consumer<SearchConfig> { this.searchByQuery(it) })
        searchController!!.setSearchableProperties(SearchableProperties.MOD_PROPERTIES)
        searchController!!.setSortConfig(preferencesService.preferences!!.vaultPrefs.modVaultConfigProperty())
    }

    private fun searchByQuery(searchConfig: SearchConfig) {
        val newSearchConfig = SearchConfig(searchConfig.getSortConfig(), searchConfig.getSearchQuery() + ";latestVersion.hidden==\"false\"")
        currentPage = 0
        enterLoadingState()
        displayModsFromSupplier({ modService.findByQuery(newSearchConfig, ++currentPage, MAX_SEARCH_RESULTS) })
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (initialized) {
            return
        }
        initialized = true

        displayShowroomMods()
    }

    private fun displayShowroomMods() {
        enterLoadingState()
        modService.getNewestMods(TOP_ELEMENT_COUNT, 1).thenAccept { mods -> replaceSearchResult(mods, newestPane) }
                .thenCompose { aVoid -> modService.getHighestRatedMods(TOP_ELEMENT_COUNT, 1) }.thenAccept { mods -> replaceSearchResult(mods, highestRatedPane) }
                .thenCompose { aVoid -> modService.getHighestRatedUiMods(TOP_ELEMENT_COUNT, 1) }.thenAccept { mods -> replaceSearchResult(mods, highestRatedUiPane) }
                .thenRun { this.enterShowroomState() }
                .exceptionally { throwable ->
                    logger.warn("Could not populate mods", throwable)
                    null
                }
    }

    private fun replaceSearchResult(modVersions: List<ModVersion>, pane: Pane?) {
        Platform.runLater { pane!!.children.clear() }
        appendSearchResult(modVersions, pane!!)
    }

    private fun enterLoadingState() {
        state = ModVaultController.State.LOADING
        showroomGroup!!.isVisible = false
        searchResultGroup!!.isVisible = false
        loadingLabel!!.isVisible = true
        backButton!!.isVisible = true
        moreButton!!.isVisible = false
    }

    private fun enterSearchResultState() {
        state = ModVaultController.State.SEARCH_RESULT
        showroomGroup!!.isVisible = false
        searchResultGroup!!.isVisible = true
        loadingLabel!!.isVisible = false
        backButton!!.isVisible = true
        moreButton!!.isVisible = searchResultPane!!.children.size % MAX_SEARCH_RESULTS == 0
    }

    private fun enterShowroomState() {
        state = ModVaultController.State.SHOWROOM
        showroomGroup!!.isVisible = true
        searchResultGroup!!.isVisible = false
        loadingLabel!!.isVisible = false
        backButton!!.isVisible = false
        moreButton!!.isVisible = false
    }

    @VisibleForTesting
    internal fun onShowModDetail(modVersion: ModVersion) {
        modDetailController!!.setModVersion(modVersion)
        modDetailController!!.root!!.isVisible = true
        modDetailController!!.root!!.requestFocus()
    }

    fun onUploadModButtonClicked() {
        Platform.runLater {
            val directoryChooser = DirectoryChooser()
            directoryChooser.initialDirectory = preferencesService.preferences!!.forgedAlliance.modsDirectory.toFile()
            directoryChooser.title = i18n.get("modVault.upload.chooseDirectory")
            val result = directoryChooser.showDialog(root!!.scene.window)

            if (result == null) {
                return@Platform.runLater
            }
            openModUploadWindow(result!!.toPath())
        }
    }

    private fun openModUploadWindow(path: Path) {
        val modUploadController = uiService.loadFxml<ModUploadController>("theme/vault/mod/mod_upload.fxml")
        modUploadController.setModPath(path)

        val root = modUploadController.root
        val dialog = uiService.showInDialog(modVaultRoot, root, i18n.get("modVault.upload.title"))
        modUploadController.setOnCancelButtonClickedListener { dialog.close() }
    }

    fun onRefreshButtonClicked() {
        modService.evictCache()
        when (state) {
            ModVaultController.State.SHOWROOM -> displayShowroomMods()
            ModVaultController.State.SEARCH_RESULT -> {
                currentPage--
                currentSupplier!!.get()
                        .thenAccept(Consumer<List<ModVersion>> { this.displayMods(it) })
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
    fun onModUploaded(event: ModUploadedEvent) {
        onRefreshButtonClicked()
    }

    fun showMoreHighestRatedUiMods() {
        enterLoadingState()
        displayModsFromSupplier({ modService.getHighestRatedUiMods(LOAD_MORE_COUNT, ++currentPage) })
    }

    fun showMoreHighestRatedMods() {
        enterLoadingState()
        displayModsFromSupplier({ modService.getHighestRatedMods(LOAD_MORE_COUNT, ++currentPage) })
    }

    fun showMoreNewestMods() {
        enterLoadingState()
        displayModsFromSupplier({ modService.getNewestMods(LOAD_MORE_COUNT, ++currentPage) })
    }

    private fun appendSearchResult(modVersions: List<ModVersion>, pane: Pane) {
        JavaFxUtil.assertBackgroundThread()

        val children = pane.children
        val controllers = modVersions.parallelStream()
                .map { mod ->
                    val controller = uiService.loadFxml<ModCardController>("theme/vault/mod/mod_card.fxml")
                    controller.setModVersion(mod)
                    controller.setOnOpenDetailListener(Consumer<ModVersion> { this.onShowModDetail(it) })
                    controller
                }.collect<List<ModCardController>, Any>(Collectors.toList())

        Iterators.partition(controllers.iterator(), BATCH_SIZE).forEachRemaining { modCardControllers ->
            Platform.runLater {
                for (modCardController in modCardControllers) {
                    children.add(modCardController.root)
                }
            }
        }
    }

    private fun displayModsFromSupplier(modsSupplier: Supplier<CompletableFuture<List<ModVersion>>>) {
        currentPage = 0
        this.currentSupplier = modsSupplier
        modsSupplier.get()
                .thenAccept(Consumer<List<ModVersion>> { this.displayMods(it) })
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"),
                            i18n.get("vault.mods.searchError"),
                            throwable,
                            i18n,
                            reportingService
                    ))
                    enterShowroomState()
                    null
                }
    }

    private fun displayMods(modVersions: List<ModVersion>) {
        Platform.runLater { searchResultPane!!.children.clear() }
        appendSearchResult(modVersions, searchResultPane!!)
        Platform.runLater { this.enterSearchResultState() }
    }

    fun onLoadMoreButtonClicked() {
        moreButton!!.isVisible = false
        loadingLabel!!.isVisible = true

        currentSupplier!!.get()
                .thenAccept { mods ->
                    appendSearchResult(mods, searchResultPane!!)
                    enterSearchResultState()
                }
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"), i18n.get("vault.mods.searchError"),
                            throwable, i18n, reportingService
                    ))
                    enterShowroomState()
                    null
                }
    }

    private enum class State {
        LOADING, SHOWROOM, SEARCH_RESULT
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
