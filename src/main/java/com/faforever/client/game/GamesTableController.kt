package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.StringCell
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.domain.RatingRange
import com.faforever.client.theme.UiService
import com.google.common.base.Joiner
import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.collections.transformation.SortedList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.SortEvent
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.control.TableColumn.SortType
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.image.Image
import javafx.util.Pair
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.stream.Collectors

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class GamesTableController @Inject
constructor(private val mapService: MapService, private val joinGameHelper: JoinGameHelper, private val i18n: I18n, private val uiService: UiService, private val preferencesService: PreferencesService) : Controller<Node> {

    private val selectedGame: ObjectProperty<Game>
    var gamesTable: TableView<Game>? = null
    var mapPreviewColumn: TableColumn<Game, Image>? = null
    var gameTitleColumn: TableColumn<Game, String>? = null
    var playersColumn: TableColumn<Game, PlayerFill>? = null
    var ratingColumn: TableColumn<Game, RatingRange>? = null
    var modsColumn: TableColumn<Game, String>? = null
    var hostColumn: TableColumn<Game, String>? = null
    var passwordProtectionColumn: TableColumn<Game, Boolean>? = null

    override val root: Node?
        get() = gamesTable

    init {

        this.selectedGame = SimpleObjectProperty()
    }

    fun selectedGameProperty(): ObjectProperty<Game> {
        return selectedGame
    }

    fun initializeGameTable(games: ObservableList<Game>) {
        val sortedList = SortedList(games)
        sortedList.comparatorProperty().bind(gamesTable!!.comparatorProperty())
        gamesTable!!.placeholder = Label(i18n.get("games.noGamesAvailable"))
        gamesTable!!.setRowFactory { param1 -> gamesRowFactory() }
        gamesTable!!.setItems(sortedList)

        applyLastSorting(gamesTable!!)
        gamesTable!!.setOnSort(EventHandler<SortEvent<TableView<Game>>> { this.onColumnSorted(it) })

        JavaFxUtil.addListener(sortedList) { observable: Observable -> selectFirstGame() }
        selectFirstGame()

        passwordProtectionColumn!!.setCellValueFactory { param -> param.value.passwordProtectedProperty() }
        passwordProtectionColumn!!.setCellFactory { param -> passwordIndicatorColumn() }
        mapPreviewColumn!!.setCellFactory { param -> MapPreviewTableCell(uiService) }
        mapPreviewColumn!!.setCellValueFactory { param ->
            Bindings.createObjectBinding(
                    { mapService.loadPreview(param.value.mapFolderName, PreviewSize.SMALL) },
                    param.value.mapFolderNameProperty()
            )
        }

        gameTitleColumn!!.setCellValueFactory { param -> param.value.titleProperty() }
        gameTitleColumn!!.setCellFactory { param -> StringCell { title -> title } }
        playersColumn!!.setCellValueFactory { param ->
            Bindings.createObjectBinding(
                    { PlayerFill(param.value.numPlayers, param.value.maxPlayers) },
                    param.value.numPlayersProperty(), param.value.maxPlayersProperty())
        }
        playersColumn!!.setCellFactory { param -> playersCell() }
        ratingColumn!!.setCellValueFactory { param -> SimpleObjectProperty(RatingRange(param.value.minRating, param.value.maxRating)) }
        ratingColumn!!.setCellFactory { param -> ratingTableCell() }
        hostColumn!!.setCellValueFactory { param -> param.value.hostProperty() }
        hostColumn!!.setCellFactory { param -> StringCell(Function<String, String> { it.toString() }) }
        modsColumn!!.setCellValueFactory(Callback<CellDataFeatures<Game, String>, ObservableValue<String>> { this.modCell(it) })
        modsColumn!!.setCellFactory { param -> StringCell(Function<String, String> { it.toString() }) }

        gamesTable!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> Platform.runLater { selectedGame.set(newValue) } }
    }

    fun setModsColumnVisibility(isVisible: Boolean) {
        modsColumn!!.isVisible = isVisible
    }

    fun setPasswordProtectionColumnVisibility(isVisible: Boolean) {
        passwordProtectionColumn!!.isVisible = isVisible
    }

    private fun applyLastSorting(gamesTable: TableView<Game>) {
        val lookup = HashMap<String, SortType>()
        val sortOrder = gamesTable.sortOrder
        preferencesService.preferences!!.gameListSorting.forEach { sorting -> lookup[sorting.key] = sorting.value }
        sortOrder.clear()
        gamesTable.columns.forEach { gameTableColumn ->
            if (lookup.containsKey(gameTableColumn.id)) {
                gameTableColumn.sortType = lookup[gameTableColumn.id]
                sortOrder.add(gameTableColumn)
            }
        }
    }

    private fun onColumnSorted(event: SortEvent<TableView<Game>>) {
        val sorters = event.source.sortOrder
                .stream()
                .map { column -> Pair(column.id, column.sortType) }
                .collect<List<Pair<String, SortType>>, Any>(Collectors.toList())

        preferencesService.preferences!!.gameListSorting.setAll(sorters)
        preferencesService.storeInBackground()
    }

    private fun modCell(param: CellDataFeatures<Game, String>): ObservableValue<String> {
        val simModCount = param.value.simMods.size
        val modNames = param.value.simMods.entries.stream()
                .limit(2)
                .map<String>(Function<Entry<String, String>, String> { it.value })
                .collect<List<String>, Any>(Collectors.toList())
        return if (simModCount > 2) {
            SimpleStringProperty(i18n.get("game.mods.twoAndMore", modNames[0], modNames.size))
        } else SimpleStringProperty(Joiner.on(i18n.get("textSeparator")).join(modNames))
    }

    private fun selectFirstGame() {
        val selectionModel = gamesTable!!.selectionModel
        if (selectionModel.selectedItem == null && !gamesTable!!.items.isEmpty()) {
            Platform.runLater { selectionModel.select(0) }
        }
    }

    private fun gamesRowFactory(): TableRow<Game> {
        val row = TableRow<Game>()
        row.setOnMouseClicked { event ->
            if (event.clickCount == 2) {
                val game = row.item
                joinGameHelper.join(game)
            }
        }
        return row
    }

    private fun passwordIndicatorColumn(): TableCell<Game, Boolean> {
        return StringCell(
                { isPasswordProtected -> if (isPasswordProtected) "\uD83D\uDD12" else "" },
                Pos.CENTER, UiService.CSS_CLASS_ICON)
    }

    private fun playersCell(): TableCell<Game, PlayerFill> {
        return StringCell({ playerFill ->
            i18n.get("game.players.format",
                    playerFill.getPlayers(), playerFill.getMaxPlayers())
        }, Pos.CENTER)
    }

    private fun ratingTableCell(): TableCell<Game, RatingRange> {
        return StringCell({ ratingRange ->
            if (ratingRange.min == null && ratingRange.max == null) {
                return ""
            }

            if (ratingRange.min != null && ratingRange.max != null) {
                return i18n.get("game.ratingFormat.minMax", ratingRange.min, ratingRange.max)
            }

            if (ratingRange.min != null) {
                return i18n.get("game.ratingFormat.minOnly", ratingRange.min)
            }

            i18n.get("game.ratingFormat.maxOnly", ratingRange.max)
        }, Pos.CENTER)
    }
}
