package com.faforever.client.replay

import com.faforever.client.api.dto.Game
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.ShowReplayEvent
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.query.SearchableProperties
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.vault.search.SearchController
import com.faforever.client.vault.search.SearchController.SearchConfig
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.lang.invoke.MethodHandles
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class OnlineReplayVaultController(private val replayService: ReplayService, private val uiService: UiService, private val notificationService: NotificationService, private val i18n: I18n, private val preferencesService: PreferencesService, private val reportingService: ReportingService) : AbstractViewController<Node>() {

    var replayVaultRoot: Pane? = null
    var newestPane: Pane? = null
    var highestRatedPane: Pane? = null
    var mostWatchedPane: Pane? = null
    var searchResultGroup: VBox? = null
    var searchResultPane: Pane? = null
    var showroomGroup: Pane? = null
    var loadingPane: VBox? = null
    var contentPane: VBox? = null
    var backButton: Button? = null
    var scrollPane: ScrollPane? = null
    var searchController: SearchController? = null
    var moreButton: Button? = null

    private var replayDetailController: ReplayDetailController? = null
    private var currentPage: Int = 0
    private var currentSupplier: Supplier<CompletableFuture<List<Replay>>>? = null
    private val state: ObjectProperty<State>

    override val root: Node?
        get() = replayVaultRoot

    init {

        state = SimpleObjectProperty(State.UNINITIALIZED)
    }

    override fun initialize() {
        super.initialize()
        JavaFxUtil.fixScrollSpeed(scrollPane!!)
        loadingPane!!.managedProperty().bind(loadingPane!!.visibleProperty())
        showroomGroup!!.managedProperty().bind(showroomGroup!!.visibleProperty())
        searchResultGroup!!.managedProperty().bind(searchResultGroup!!.visibleProperty())
        backButton!!.managedProperty().bind(backButton!!.visibleProperty())
        moreButton!!.managedProperty().bind(moreButton!!.visibleProperty())

        searchController!!.setRootType(Game::class.java)
        searchController!!.setSearchListener(Consumer<SearchConfig> { this.onSearch(it) })
        searchController!!.setSearchableProperties(SearchableProperties.GAME_PROPERTIES)
        searchController!!.setSortConfig(preferencesService.preferences!!.vaultPrefs.onlineReplaySortConfigProperty())
    }

    private fun displaySearchResult(replays: List<Replay>, append: Boolean = false) {
        showroomGroup!!.isVisible = false
        searchResultGroup!!.isVisible = true
        loadingPane!!.isVisible = false
        backButton!!.isVisible = true
        populateReplays(replays, searchResultPane!!, append)
        moreButton!!.isVisible = replays.size == MAX_SEARCH_RESULTS
    }

    private fun populateReplays(replays: List<Replay>, pane: Pane, append: Boolean) {
        val children = pane.children
        Platform.runLater {
            if (!append) {
                children.clear()
            }
            replays.forEach { replay ->
                val controller = uiService.loadFxml<ReplayCardController>("theme/vault/replay/replay_card.fxml")
                controller.setReplay(replay)
                controller.setOnOpenDetailListener(Consumer<Replay> { this.onShowReplayDetail(it) })
                children.add(controller.root)

                if (replays.size == 1 && !append) {
                    onShowReplayDetail(replay)
                }
            }
        }
    }

    fun populateReplays(replays: List<Replay>, pane: Pane?) {
        populateReplays(replays, pane!!, false)
    }

    fun onShowReplayDetail(replay: Replay) {
        replayDetailController = uiService.loadFxml("theme/vault/replay/replay_detail.fxml")
        replayDetailController!!.setReplay(replay)

        val replayDetailRoot = replayDetailController!!.root
        replayDetailRoot!!.isVisible = true
        replayDetailRoot.requestFocus()

        replayVaultRoot!!.children.add(replayDetailRoot)
        AnchorPane.setTopAnchor(replayDetailRoot, 0.0)
        AnchorPane.setRightAnchor(replayDetailRoot, 0.0)
        AnchorPane.setBottomAnchor(replayDetailRoot, 0.0)
        AnchorPane.setLeftAnchor(replayDetailRoot, 0.0)
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (state.get() == State.UNINITIALIZED) {
            if (navigateEvent is ShowReplayEvent) {
                state.addListener(object : ChangeListener<State> {
                    override fun changed(observable: ObservableValue<out State>, oldValue: State, newValue: State) {
                        if (newValue != State.UNINITIALIZED) {
                            Platform.runLater { onShowReplayDetail(navigateEvent.getReplay()) }
                            state.removeListener(this)
                        }
                    }
                })
            }
            refresh()
            return
        }
        if (navigateEvent is ShowReplayEvent) {
            onShowReplayDetail(navigateEvent.getReplay())
        }
    }

    private fun enterSearchingState() {
        state.set(State.SEARCHING)

        showroomGroup!!.isVisible = false
        searchResultGroup!!.isVisible = false
        loadingPane!!.isVisible = true
        backButton!!.isVisible = false
        moreButton!!.isVisible = false
    }

    private fun enterResultState() {
        state.set(State.RESULT)

        showroomGroup!!.isVisible = true
        searchResultGroup!!.isVisible = false
        loadingPane!!.isVisible = false
        backButton!!.isVisible = false
        moreButton!!.isVisible = false
    }

    private fun onSearch(searchConfig: SearchConfig) {
        enterSearchingState()
        displayReplaysFromSupplier({ replayService.findByQuery(searchConfig.getSearchQuery(), MAX_SEARCH_RESULTS, currentPage++, searchConfig.getSortConfig()) })
    }

    fun onBackButtonClicked() {
        enterResultState()
    }

    fun onRefreshButtonClicked() {
        refresh()
    }

    private fun refresh() {
        enterSearchingState()
        replayService.getNewestReplays(TOP_ELEMENT_COUNT, 1)
                .thenAccept { replays -> populateReplays(replays, newestPane) }
                .thenCompose { aVoid -> replayService.getHighestRatedReplays(TOP_ELEMENT_COUNT, 1).thenAccept { modInfoBeans -> populateReplays(modInfoBeans, highestRatedPane) } }
                .thenRun { this.enterResultState() }
                .exceptionally { throwable ->
                    logger.warn("Could not populate replays", throwable)
                    null
                }
    }

    fun onMoreNewestButtonClicked() {
        enterSearchingState()
        displayReplaysFromSupplier({ replayService.getNewestReplays(TOP_MORE_ELEMENT_COUNT, currentPage++) })
    }

    fun onMoreHighestRatedButtonClicked() {
        enterSearchingState()
        displayReplaysFromSupplier({ replayService.getHighestRatedReplays(TOP_MORE_ELEMENT_COUNT, currentPage++) })
    }

    fun onLoadMoreButtonClicked(actionEvent: ActionEvent) {
        currentSupplier!!.get()
                .thenAccept { replays -> displaySearchResult(replays, true) }
    }

    private fun displayReplaysFromSupplier(mapsSupplier: Supplier<CompletableFuture<List<Replay>>>) {
        currentPage = 1
        this.currentSupplier = mapsSupplier
        mapsSupplier.get()
                .thenAccept(Consumer<List<Replay>> { this.displaySearchResult(it) })
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"), i18n.get("vault.replays.searchError"), throwable, i18n, reportingService
                    ))
                    enterResultState()
                    null
                }
    }

    private enum class State {
        SEARCHING, RESULT, UNINITIALIZED
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val TOP_ELEMENT_COUNT = 10
        private val TOP_MORE_ELEMENT_COUNT = 100
        private val MAX_SEARCH_RESULTS = 100
    }
}
