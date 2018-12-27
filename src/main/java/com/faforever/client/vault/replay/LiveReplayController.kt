package com.faforever.client.vault.replay

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.NodeTableCell
import com.faforever.client.fx.StringCell
import com.faforever.client.game.Game
import com.faforever.client.game.GameService
import com.faforever.client.game.MapPreviewTableCell
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.theme.UiService
import com.google.common.base.Joiner
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.collections.transformation.SortedList
import javafx.scene.Node
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.control.TableView
import javafx.scene.image.Image
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.collections.Map.Entry
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class LiveReplayController(private val gameService: GameService, private val uiService: UiService, private val i18n: I18n, private val mapService: MapService) : AbstractViewController<Node>() {
    private val selectedGame: ObjectProperty<Game>
    var liveReplayControllerRoot: TableView<Game>? = null
    var mapPreviewColumn: TableColumn<Game, Image>? = null
    var gameTitleColumn: TableColumn<Game, String>? = null
    var playersColumn: TableColumn<Game, Number>? = null
    var modsColumn: TableColumn<Game, String>? = null
    var hostColumn: TableColumn<Game, String>? = null
    var watchColumn: TableColumn<Game, Game>? = null

    override val root: Node?
        get() = liveReplayControllerRoot

    init {

        selectedGame = SimpleObjectProperty()
    }

    override fun initialize() {
        initializeGameTable(gameService.games.filtered { game -> game.status == GameStatus.PLAYING })
    }

    private fun initializeGameTable(games: ObservableList<Game>) {
        val sortedList = SortedList(games)
        sortedList.comparatorProperty().bind(liveReplayControllerRoot!!.comparatorProperty())

        mapPreviewColumn!!.setCellFactory { param -> MapPreviewTableCell(uiService) }
        mapPreviewColumn!!.setCellValueFactory { param ->
            Bindings.createObjectBinding(
                    { mapService.loadPreview(param.value.mapFolderName, PreviewSize.SMALL) },
                    param.value.mapFolderNameProperty()
            )
        }

        gameTitleColumn!!.setCellValueFactory { param -> param.value.titleProperty() }
        gameTitleColumn!!.setCellFactory { param -> StringCell { title -> title } }
        playersColumn!!.setCellValueFactory { param -> param.value.numPlayersProperty() }
        playersColumn!!.setCellFactory { param -> StringCell { number -> i18n.number(number.toInt()) } }
        hostColumn!!.setCellValueFactory { param -> param.value.hostProperty() }
        hostColumn!!.setCellFactory { param -> StringCell(Function<String, String> { it.toString() }) }
        modsColumn!!.setCellValueFactory(Callback<CellDataFeatures<Game, String>, ObservableValue<String>> { this.modCell(it) })
        modsColumn!!.setCellFactory { param -> StringCell(Function<String, String> { it.toString() }) }
        watchColumn!!.setCellValueFactory { param -> SimpleObjectProperty(param.value) }
        watchColumn!!.setCellFactory { param -> NodeTableCell(Function<Game, Node> { this.watchReplayButton(it) }) }

        liveReplayControllerRoot!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> Platform.runLater { selectedGame.set(newValue) } }

        liveReplayControllerRoot!!.setItems(games)
    }

    private fun watchReplayButton(game: Game): Node? {
        val controller = uiService.loadFxml<WatchButtonController>("theme/vault/replay/watch_button.fxml")
        controller.setGame(game)
        return controller.root
    }

    private fun modCell(param: CellDataFeatures<Game, String>): ObservableValue<String> {
        val simMods = param.value.simMods
        val simModCount = simMods.size
        val modNames = simMods.entries.stream()
                .limit(2)
                .map<String>(Function<Entry<String, String>, String> { it.value })
                .collect<List<String>, Any>(Collectors.toList())
        return if (simModCount > 2) {
            SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames[0], modNames.size))
        } else SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames))
    }

}
