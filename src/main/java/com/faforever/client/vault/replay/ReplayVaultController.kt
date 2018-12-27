package com.faforever.client.vault.replay

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapBean
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.notification.DismissAction
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.PersistentNotification
import com.faforever.client.notification.ReportAction
import com.faforever.client.notification.Severity
import com.faforever.client.replay.LoadLocalReplaysTask
import com.faforever.client.replay.Replay
import com.faforever.client.replay.ReplayService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.task.TaskService
import com.faforever.client.theme.UiService
import com.faforever.client.util.TimeService
import com.faforever.client.vault.map.MapPreviewTableCellController
import com.google.common.base.Joiner
import javafx.application.Platform
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.image.ImageView
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.Temporal
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

import java.util.Arrays.asList

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReplayVaultController @Inject
constructor(private val notificationService: NotificationService, private val replayService: ReplayService, private val mapService: MapService, private val taskService: TaskService, private val i18n: I18n, private val timeService: TimeService, private val reportingService: ReportingService, private val applicationContext: ApplicationContext, private val uiService: UiService)// TODO reduce dependencies
    : Controller<Node> {

    var replayVaultRoot: TableView<Replay>? = null
    var idColumn: TableColumn<Replay, Number>? = null
    var titleColumn: TableColumn<Replay, String>? = null
    var playersColumn: TableColumn<Replay, String>? = null
    var timeColumn: TableColumn<Replay, Temporal>? = null
    var durationColumn: TableColumn<Replay, Duration>? = null
    var gameTypeColumn: TableColumn<Replay, String>? = null
    var mapColumn: TableColumn<Replay, MapBean>? = null

    //  public void loadOnlineReplaysInBackground() {
    //    replayService.getOnlineReplays()
    //        .thenAccept(this::addOnlineReplays)
    //        .exceptionally(throwable -> {
    //          logger.warn("Error while loading online replays", throwable);
    //          notificationService.addNotification(new PersistentNotification(
    //              i18n.valueOf("replays.loadingOnlineTask.failed"),
    //              Severity.ERROR,
    //              Collections.singletonList(new Action(i18n.valueOf("report"), event -> reportingService.reportError(throwable)))
    //          ));
    //          return null;
    //        });
    //  }

    //  private void addOnlineReplays(Collection<ReplayInfoBean> result) {
    //    Collection<Item<ReplayInfoBean>> items = result.stream()
    //        .map(Item::new).collect(Collectors.toCollection(ArrayList::new));
    //    Platform.runLater(() -> onlineReplaysRoot.getChildren().addAll(items));
    //  }

    override val root: Node?
        get() = replayVaultRoot

    override fun initialize() {

        replayVaultRoot!!.setRowFactory { param -> replayRowFactory() }
        replayVaultRoot!!.sortOrder.setAll(listOf<TableColumn<Replay, Temporal>>(timeColumn))

        idColumn!!.setCellValueFactory { param -> param.value.idProperty() }
        idColumn!!.setCellFactory(Callback<TableColumn<Replay, Number>, TableCell<Replay, Number>> { this.idCellFactory(it) })

        titleColumn!!.setCellValueFactory { param -> param.value.titleProperty() }

        timeColumn!!.setCellValueFactory { param -> param.value.startTimeProperty() }
        timeColumn!!.setCellFactory(Callback<TableColumn<Replay, Temporal>, TableCell<Replay, Temporal>> { this.timeCellFactory(it) })
        timeColumn!!.sortType = TableColumn.SortType.DESCENDING

        gameTypeColumn!!.setCellValueFactory { param -> param.value.featuredMod.displayNameProperty() }

        mapColumn!!.setCellValueFactory { param -> param.value.mapProperty() }
        mapColumn!!.setCellFactory(Callback<TableColumn<Replay, MapBean>, TableCell<Replay, MapBean>> { this.mapCellFactory(it) })

        playersColumn!!.setCellValueFactory(Callback<CellDataFeatures<Replay, String>, ObservableValue<String>> { this.playersValueFactory(it) })

        durationColumn!!.setCellValueFactory(Callback<CellDataFeatures<Replay, Duration>, ObservableValue<Duration>> { this.durationCellValueFactory(it) })
        durationColumn!!.setCellFactory(Callback<TableColumn<Replay, Duration>, TableCell<Replay, Duration>> { this.durationCellFactory(it) })

        loadLocalReplaysInBackground()
    }

    private fun replayRowFactory(): TableRow<Replay> {
        val row = TableRow<Replay>()
        row.setOnMouseClicked { event ->
            // If ID == 0, this isn't an entry but root node
            if (event.clickCount == 2 && !row.isEmpty && row.item.id != 0) {
                replayService.runReplay(row.item)
            }
        }
        return row
    }

    private fun playersValueFactory(features: TableColumn.CellDataFeatures<Replay, String>): ObservableValue<String> {
        return object : StringBinding() {
            override fun computeValue(): String {
                val replay = features.value

                val teams = replay.teams
                if (teams.isEmpty()) {
                    return ""
                }

                val teamsAsStrings = ArrayList<String>()
                for (playerNames in teams.values) {
                    Collections.sort(playerNames)
                    teamsAsStrings.add(Joiner.on(i18n.get("textSeparator")).join(playerNames))
                }

                return Joiner.on(i18n.get("vsSeparator")).join(teamsAsStrings)
            }
        }
    }

    private fun timeCellFactory(column: TableColumn<Replay, Temporal>): TableCell<Replay, Temporal> {
        val cell = TextFieldTableCell<Replay, Temporal>()
        cell.converter = object : StringConverter<Temporal>() {
            override fun toString(`object`: Temporal): String {
                return timeService.lessThanOneDayAgo(`object`)
            }

            override fun fromString(string: String): OffsetDateTime? {
                return null
            }
        }
        return cell
    }

    private fun mapCellFactory(column: TableColumn<Replay, MapBean>): TableCell<Replay, MapBean> {
        val controller = uiService.loadFxml<MapPreviewTableCellController>("theme/vault/map/map_preview_table_cell.fxml")
        val imageView = controller.root

        val cell = object : TableCell<Replay, MapBean>() {

            override fun updateItem(map: MapBean?, empty: Boolean) {
                super.updateItem(map, empty)

                if (empty || map == null) {
                    text = null
                    graphic = null
                } else {
                    imageView!!.image = mapService.loadPreview(map.folderName, PreviewSize.SMALL)
                    graphic = imageView
                    text = map.displayName
                }
            }
        }
        cell.graphic = imageView

        return cell
    }

    private fun idCellFactory(column: TableColumn<Replay, Number>): TableCell<Replay, Number> {
        val cell = TextFieldTableCell<Replay, Number>()
        cell.converter = object : StringConverter<Number>() {
            override fun toString(`object`: Number): String {
                return if (`object`.toInt() == 0) {
                    ""
                } else i18n.number(`object`.toInt())
            }

            override fun fromString(string: String): Number? {
                return null
            }
        }
        return cell
    }

    private fun durationCellFactory(column: TableColumn<Replay, Duration>): TableCell<Replay, Duration> {
        val cell = TextFieldTableCell<Replay, Duration>()
        cell.converter = object : StringConverter<Duration>() {
            override fun toString(`object`: Duration?): String {
                return if (`object` == null) {
                    ""
                } else timeService.shortDuration(`object`)
            }

            override fun fromString(string: String): Duration? {
                return null
            }
        }
        return cell
    }

    private fun durationCellValueFactory(param: TableColumn.CellDataFeatures<Replay, Duration>): ObservableValue<Duration> {
        val replay = param.value
        val startTime = replay.startTime
        val endTime = replay.endTime

        return if (startTime == null || endTime == null) {
            SimpleObjectProperty(null)
        } else SimpleObjectProperty(Duration.between(startTime, endTime))

    }

    fun loadLocalReplaysInBackground(): CompletableFuture<Void> {
        // TODO use replay service
        val task = applicationContext.getBean(LoadLocalReplaysTask::class.java)

        replayVaultRoot!!.items.clear()
        return taskService.submitTask(task).future
                .thenAccept(Consumer<Collection<Replay>> { this.addLocalReplays(it) })
                .exceptionally { throwable ->
                    logger.warn("Error while loading local replays", throwable)
                    notificationService.addNotification(PersistentNotification(
                            i18n.get("replays.loadingLocalTask.failed"),
                            Severity.ERROR, asList<Action>(ReportAction(i18n, reportingService, throwable), DismissAction(i18n))
                    ))
                    null
                }
    }

    private fun addLocalReplays(result: Collection<Replay>) {
        val items = result.stream()
                .collect<ArrayList<Replay>, Any>(Collectors.toCollection(Supplier<ArrayList<Replay>> { ArrayList() }))
        Platform.runLater { replayVaultRoot!!.items.addAll(items) }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
