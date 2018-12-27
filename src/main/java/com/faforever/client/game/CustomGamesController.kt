package com.faforever.client.game

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.HostGameEvent
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.theme.UiService
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import com.jfoenix.controls.JFXDialog
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.ScrollPane
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.util.StringConverter
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.nio.file.Path
import java.util.Arrays
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class CustomGamesController(private val uiService: UiService, private val gameService: GameService, private val preferencesService: PreferencesService,
                            private val eventBus: EventBus, private val i18n: I18n) : AbstractViewController<Node>() {

    var gameDetailController: GameDetailController? = null
    private var gamesTableController: GamesTableController? = null

    var tableButton: ToggleButton? = null
    var tilesButton: ToggleButton? = null
    var viewToggleGroup: ToggleGroup? = null
    var createGameButton: Button? = null
    var gameViewContainer: Pane? = null
    var gamesRoot: StackPane? = null
    var gameDetailPane: ScrollPane? = null
    var chooseSortingTypeChoiceBox: ComboBox<TilesSortingOrder>? = null

    @VisibleForTesting
    internal var filteredItems: FilteredList<Game>

    var showModdedGamesCheckBox: CheckBox? = null
    var showPasswordProtectedGamesCheckBox: CheckBox? = null
    private val filterConditionsChangedListener = { observable, oldValue, newValue -> updateFilteredItems() }
    private var gamesTilesContainerController: GamesTilesContainerController? = null

    override val root: Node?
        get() = gamesRoot

    override fun initialize() {
        JavaFxUtil.bind(createGameButton!!.disableProperty(), gameService.gameRunningProperty())
        root!!.sceneProperty().addListener { observable, oldValue, newValue ->
            if (newValue == null) {
                createGameButton!!.disableProperty().unbind()
            }
        }

        chooseSortingTypeChoiceBox!!.isVisible = false
        chooseSortingTypeChoiceBox!!.setConverter(object : StringConverter() {
            fun toString(tilesSortingOrder: TilesSortingOrder?): String {
                return if (tilesSortingOrder == null) "null" else i18n.get(tilesSortingOrder.getDisplayNameKey())
            }

            override fun fromString(string: String): TilesSortingOrder {
                throw UnsupportedOperationException("Not supported")
            }
        })

        val games = gameService.games

        filteredItems = FilteredList(games)
        JavaFxUtil.bindBidirectional(showModdedGamesCheckBox!!.selectedProperty(), preferencesService.preferences!!.showModdedGamesProperty())
        JavaFxUtil.bindBidirectional(showPasswordProtectedGamesCheckBox!!.selectedProperty(), preferencesService.preferences!!.showPasswordProtectedGamesProperty())

        updateFilteredItems()
        JavaFxUtil.addListener(preferencesService.preferences!!.showModdedGamesProperty(), WeakChangeListener(filterConditionsChangedListener))
        JavaFxUtil.addListener(preferencesService.preferences!!.showPasswordProtectedGamesProperty(), WeakChangeListener(filterConditionsChangedListener))

        if (tilesButton!!.id == preferencesService.preferences!!.gamesViewMode) {
            viewToggleGroup!!.selectToggle(tilesButton)
            tilesButton!!.onAction.handle(null)
        } else {
            viewToggleGroup!!.selectToggle(tableButton)
            tableButton!!.onAction.handle(null)
            gamesTableController!!.setModsColumnVisibility(showModdedGamesCheckBox!!.selectedProperty().value!!)
            gamesTableController!!.setPasswordProtectionColumnVisibility(showPasswordProtectedGamesCheckBox!!.selectedProperty().value!!)
        }
        viewToggleGroup!!.selectedToggleProperty().addListener { observable, oldValue, newValue ->
            if (newValue == null) {
                if (oldValue != null) {
                    viewToggleGroup!!.selectToggle(oldValue)
                } else {
                    viewToggleGroup!!.selectToggle(viewToggleGroup!!.toggles[0])
                }
                return@viewToggleGroup.selectedToggleProperty().addListener
            }
            preferencesService.preferences!!.gamesViewMode = (newValue as ToggleButton).id
            preferencesService.storeInBackground()
        }

        setSelectedGame(null)
        eventBus.register(this)
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (navigateEvent is HostGameEvent) {
            onCreateGame(navigateEvent.getMapFolderName())
        }
        updateFilteredItems()
    }

    private fun updateFilteredItems() {
        preferencesService.storeInBackground()

        val showPasswordProtectedGames = showPasswordProtectedGamesCheckBox!!.isSelected
        val showModdedGames = showModdedGamesCheckBox!!.isSelected

        filteredItems.setPredicate(OPEN_CUSTOM_GAMES_PREDICATE.and { gameInfoBean -> (showPasswordProtectedGames || !gameInfoBean.getPasswordProtected()) && (showModdedGames || gameInfoBean.getSimMods().isEmpty()) })
        if (tableButton!!.isSelected) {
            gamesTableController!!.setModsColumnVisibility(showModdedGamesCheckBox!!.selectedProperty().value!!)
            gamesTableController!!.setPasswordProtectionColumnVisibility(showPasswordProtectedGamesCheckBox!!.selectedProperty().value!!)
        }
    }

    fun onCreateGameButtonClicked() {
        onCreateGame(null)
    }

    private fun onCreateGame(mapFolderName: String?) {
        if (preferencesService.preferences!!.forgedAlliance.path == null) {
            val gameDirectoryFuture = CompletableFuture<Path>()
            eventBus.post(GameDirectoryChooseEvent(gameDirectoryFuture))
            gameDirectoryFuture.thenAccept { path -> Optional.ofNullable(path).ifPresent { path1 -> onCreateGame(null) } }
            return
        }

        val createGameController = uiService.loadFxml<CreateGameController>("theme/play/create_game.fxml")

        if (mapFolderName != null && !createGameController.selectMap(mapFolderName)) {
            log.warn("Map with folder name '{}' could not be found in map list", mapFolderName)
        }

        val root = createGameController.root
        val dialog = uiService.showInDialog(gamesRoot, root, i18n.get("games.create"))
        createGameController.setOnCloseButtonClickedListener { dialog.close() }

        root!!.requestFocus()
    }

    fun onTableButtonClicked() {
        gamesTableController = uiService.loadFxml("theme/play/games_table.fxml")
        gamesTableController!!.selectedGameProperty().addListener { observable, oldValue, newValue -> setSelectedGame(newValue) }
        Platform.runLater {
            gamesTableController!!.initializeGameTable(filteredItems)

            val root = gamesTableController!!.root
            populateContainer(root)
        }
    }

    private fun populateContainer(root: Node?) {
        chooseSortingTypeChoiceBox!!.isVisible = false
        gameViewContainer!!.children.setAll(root)
        AnchorPane.setBottomAnchor(root, 0.0)
        AnchorPane.setLeftAnchor(root, 0.0)
        AnchorPane.setRightAnchor(root, 0.0)
        AnchorPane.setTopAnchor(root, 0.0)
    }

    fun onTilesButtonClicked() {
        gamesTilesContainerController = uiService.loadFxml("theme/play/games_tiles_container.fxml")
        gamesTilesContainerController!!.selectedGameProperty().addListener { observable, oldValue, newValue -> setSelectedGame(newValue) }
        chooseSortingTypeChoiceBox!!.items.clear()

        Platform.runLater {
            val root = gamesTilesContainerController!!.root
            populateContainer(root)
            gamesTilesContainerController!!.createTiledFlowPane(filteredItems, chooseSortingTypeChoiceBox)
        }
    }

    @VisibleForTesting
    internal fun setSelectedGame(game: Game?) {
        gameDetailController!!.setGame(game)
        if (game == null) {
            gameDetailPane!!.isVisible = false
            return
        }

        gameDetailPane!!.isVisible = true
    }

    @VisibleForTesting
    internal fun setFilteredList(games: ObservableList<Game>) {
        filteredItems = FilteredList(games) { s -> true }
    }

    override fun onHide() {
        // Hide all games to free up memory
        filteredItems.setPredicate { game -> false }
    }

    companion object {

        private val HIDDEN_FEATURED_MODS = Arrays.asList(
                KnownFeaturedMod.COOP.technicalName,
                KnownFeaturedMod.LADDER_1V1.technicalName,
                KnownFeaturedMod.GALACTIC_WAR.technicalName,
                KnownFeaturedMod.MATCHMAKER.technicalName
        )

        private val OPEN_CUSTOM_GAMES_PREDICATE = { gameInfoBean -> gameInfoBean.getStatus() == GameStatus.OPEN && !HIDDEN_FEATURED_MODS.contains(gameInfoBean.getFeaturedMod()) }
    }
}
