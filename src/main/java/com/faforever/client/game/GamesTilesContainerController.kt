package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import com.google.common.annotations.VisibleForTesting
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.collections.WeakListChangeListener
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.ScrollPane
import javafx.scene.layout.FlowPane
import lombok.Getter
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.Comparator
import java.util.HashMap
import java.util.Locale

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class GamesTilesContainerController @Inject
constructor(private val uiService: UiService, private val preferencesService: PreferencesService) : Controller<Node> {
    private val gameListChangeListener: ListChangeListener<Game>
    var tiledFlowPane: FlowPane? = null
    var tiledScrollPane: ScrollPane? = null
    private val sortingListener: ChangeListener<in TilesSortingOrder>
    private val selectedGame: ObjectProperty<Game>
    private var appliedComparator: Comparator<Node>? = null
    @VisibleForTesting
    internal var uidToGameCard: MutableMap<Int, Node>

    override val root: Node?
        get() = tiledScrollPane

    init {
        selectedGame = SimpleObjectProperty()

        sortingListener = { observable, oldValue, newValue ->
            if (newValue == null) {
                return
            }
            preferencesService.preferences!!.gameTileSortingOrder = newValue
            preferencesService.storeInBackground()
            appliedComparator = newValue!!.getComparator()
            sortNodes()
        }

        gameListChangeListener = { change ->
            Platform.runLater {
                synchronized(change) {
                    while (change.next()) {
                        change.getRemoved().forEach { gameInfoBean -> tiledFlowPane!!.children.remove(uidToGameCard.remove(gameInfoBean.getId())) }
                        change.getAddedSubList().forEach(Consumer<out Game> { this@GamesTilesContainerController.addGameCard(it) })
                    }
                    sortNodes()
                }
            }
        }
    }

    private fun sortNodes() {
        val sortedChildren = tiledFlowPane!!.children.sorted(appliedComparator)
        tiledFlowPane!!.children.setAll(sortedChildren)
    }

    override fun initialize() {
        JavaFxUtil.fixScrollSpeed(tiledScrollPane!!)
    }

    internal fun selectedGameProperty(): ReadOnlyObjectProperty<Game> {
        return this.selectedGame
    }

    @VisibleForTesting
    internal fun createTiledFlowPane(games: ObservableList<Game>, choseSortingTypeChoiceBox: ComboBox<TilesSortingOrder>) {
        initializeChoiceBox(choseSortingTypeChoiceBox)
        uidToGameCard = HashMap()
        games.forEach(Consumer<Game> { this.addGameCard(it) })
        JavaFxUtil.addListener(games, WeakListChangeListener(gameListChangeListener))
        selectFirstGame()
        sortNodes()
    }

    private fun initializeChoiceBox(sortingTypeChoiceBox: ComboBox<TilesSortingOrder>) {
        sortingTypeChoiceBox.isVisible = true
        sortingTypeChoiceBox.items.addAll(*TilesSortingOrder.values())
        sortingTypeChoiceBox.selectionModel.selectedItemProperty().addListener(WeakChangeListener<in TilesSortingOrder>(sortingListener))
        sortingTypeChoiceBox.selectionModel.select(preferencesService.preferences!!.gameTileSortingOrder)
    }

    private fun selectFirstGame() {
        val cards = tiledFlowPane!!.children
        if (!cards.isEmpty()) {
            selectedGame.set(cards[0].userData as Game)
        }
    }

    private fun addGameCard(game: Game) {
        val gameTileController = uiService.loadFxml<GameTileController>("theme/play/game_card.fxml")
        gameTileController.setGame(game)
        gameTileController.setOnSelectedListener { selection -> selectedGame.set(selection) }

        val root = gameTileController.root
        root!!.userData = game
        tiledFlowPane!!.children.add(root)
        uidToGameCard[game.id] = root
    }

    enum class TilesSortingOrder private constructor(comparator: Comparator<Node>, reversed: Boolean, @field:Getter
    private val displayNameKey: String) {
        PLAYER_DES(Comparator.comparingInt<Node> { o -> (o.userData as Game).numPlayers }, true, "tiles.comparator.playersDescending"),
        PLAYER_ASC(Comparator.comparingInt<Node> { o -> (o.userData as Game).numPlayers }, false, "tiles.comparator.playersAscending"),
        NAME_DES(Comparator.comparing<Node, String> { o -> (o.userData as Game).title.toLowerCase(Locale.US) }, true, "tiles.comparator.nameDescending"),
        NAME_ASC(Comparator.comparing<Node, String> { o -> (o.userData as Game).title.toLowerCase(Locale.US) }, false, "tiles.comparator.nameAscending");

        @Getter
        private val comparator: Comparator<Node>

        init {
            this.comparator = if (reversed) comparator.reversed() else comparator
        }
    }
}
