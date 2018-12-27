package com.faforever.client.game

import com.faforever.client.fa.FaStrings
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.StringListCell
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapBean
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.map.MapSize
import com.faforever.client.mod.FeaturedMod
import com.faforever.client.mod.ModService
import com.faforever.client.mod.ModVersion
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.preferences.PreferenceUpdateListener
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.remote.FafService
import com.faforever.client.reporting.ReportingService
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundImage
import javafx.scene.layout.BackgroundSize
import javafx.scene.layout.Pane
import javafx.util.Callback
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.lang.ref.WeakReference
import java.util.Objects
import java.util.Optional
import java.util.stream.Collectors

import com.faforever.client.net.ConnectionState.CONNECTED
import javafx.scene.layout.BackgroundPosition.CENTER
import javafx.scene.layout.BackgroundRepeat.NO_REPEAT

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class CreateGameController @Inject
constructor(private val fafService: FafService, private val mapService: MapService, private val modService: ModService, private val gameService: GameService, private val preferencesService: PreferencesService, private val i18n: I18n, private val notificationService: NotificationService, private val reportingService: ReportingService) : Controller<Pane> {
    var mapSizeLabel: Label? = null
    var mapPlayersLabel: Label? = null
    var mapDescriptionLabel: Label? = null
    var mapNameLabel: Label? = null
    var mapSearchTextField: TextField? = null
    var titleTextField: TextField? = null
    var modListView: ListView<ModVersion>? = null
    var passwordTextField: TextField? = null
    var minRankingTextField: TextField? = null
    var maxRankingTextField: TextField? = null
    var featuredModListView: ListView<FeaturedMod>? = null
    var mapListView: ListView<MapBean>? = null
    override var root: Pane? = null
    var createGameButton: Button? = null
    var mapPreviewPane: Pane? = null
    var versionLabel: Label? = null
    var onlyForFriendsCheckBox: CheckBox? = null
    @VisibleForTesting
    internal var filteredMapBeans: FilteredList<MapBean>
    private var onCloseButtonClickedListener: Runnable? = null
    private var preferenceUpdateListener: PreferenceUpdateListener? = null
    /**
     * Remembers if the controller's init method was called, to avoid memory leaks by adding several listeners
     */
    private var initialized: Boolean = false

    override fun initialize() {
        versionLabel!!.managedProperty().bind(versionLabel!!.visibleProperty())

        mapPreviewPane!!.prefHeightProperty().bind(mapPreviewPane!!.widthProperty())
        mapSearchTextField!!.textProperty().addListener { observable, oldValue, newValue ->
            if (newValue.isEmpty()) {
                filteredMapBeans.setPredicate { mapInfoBean -> true }
            } else {
                filteredMapBeans.setPredicate { mapInfoBean -> mapInfoBean.displayName.toLowerCase().contains(newValue.toLowerCase()) || mapInfoBean.folderName.toLowerCase().contains(newValue.toLowerCase()) }
            }
            if (!filteredMapBeans.isEmpty()) {
                mapListView!!.selectionModel.select(0)
            }
        }
        mapSearchTextField!!.setOnKeyPressed { event ->
            val selectionModel = mapListView!!.selectionModel
            val currentMapIndex = selectionModel.selectedIndex
            var newMapIndex = currentMapIndex
            if (KeyCode.DOWN == event.code) {
                if (filteredMapBeans.size > currentMapIndex + 1) {
                    newMapIndex++
                }
                event.consume()
            } else if (KeyCode.UP == event.code) {
                if (currentMapIndex > 0) {
                    newMapIndex--
                }
                event.consume()
            }
            selectionModel.select(newMapIndex)
            mapListView!!.scrollTo(newMapIndex)
        }

        featuredModListView!!.setCellFactory { param -> StringListCell(Function<FeaturedMod, String> { it.getDisplayName() }) }

        JavaFxUtil.makeNumericTextField(minRankingTextField!!, MAX_RATING_LENGTH)
        JavaFxUtil.makeNumericTextField(maxRankingTextField!!, MAX_RATING_LENGTH)

        modService.featuredMods.thenAccept { featuredModBeans ->
            Platform.runLater {
                featuredModListView!!.setItems(FXCollections.observableList(featuredModBeans).filtered(Predicate<FeaturedMod> { it.isVisible() }))
                selectLastOrDefaultGameType()
            }
        }

        if (preferencesService.preferences!!.forgedAlliance.path == null) {
            preferenceUpdateListener = { preferences ->
                if (!initialized && preferencesService.preferences!!.forgedAlliance.path != null) {
                    initialized = true

                    Platform.runLater { this.init() }
                }
            }
            preferencesService.addUpdateListener(WeakReference(preferenceUpdateListener))
        } else {
            init()
        }
    }

    fun onCloseButtonClicked() {
        onCloseButtonClickedListener!!.run()
    }


    private fun init() {
        bindGameVisibility()
        initModList()
        initMapSelection()
        initFeaturedModList()
        initRatingBoundaries()
        selectLastMap()
        setLastGameTitle()
        titleTextField!!.textProperty().addListener { observable, oldValue, newValue ->
            preferencesService.preferences!!.lastGameTitle = newValue
            preferencesService.storeInBackground()
        }

        createGameButton!!.textProperty().bind(Bindings.createStringBinding({
            when (fafService.connectionStateProperty().get()) {
                ConnectionState.DISCONNECTED -> return@Bindings.createStringBinding i18n . get "game.create.disconnected"
                ConnectionState.CONNECTING -> return@Bindings.createStringBinding i18n . get "game.create.connecting"
                else -> {
                }
            }
            if (Strings.isNullOrEmpty(titleTextField!!.text)) {
                return@Bindings.createStringBinding i18n . get "game.create.titleMissing"
            } else if (featuredModListView!!.selectionModel.selectedItem == null) {
                return@Bindings.createStringBinding i18n . get "game.create.featuredModMissing"
            }
            i18n.get("game.create.create")
        }, titleTextField!!.textProperty(), featuredModListView!!.selectionModel.selectedItemProperty(), fafService.connectionStateProperty()))

        createGameButton!!.disableProperty().bind(
                titleTextField!!.textProperty().isEmpty
                        .or(featuredModListView!!.selectionModel.selectedItemProperty().isNull.or(fafService.connectionStateProperty().isNotEqualTo(CONNECTED)))
        )
    }

    private fun bindGameVisibility() {
        preferencesService.preferences!!
                .lastGameOnlyFriendsProperty()
                .bindBidirectional(onlyForFriendsCheckBox!!.selectedProperty())
        onlyForFriendsCheckBox!!.selectedProperty().addListener { observable -> preferencesService.storeInBackground() }
    }

    private fun initModList() {
        modListView!!.selectionModel.selectionMode = SelectionMode.MULTIPLE
        modListView!!.cellFactory = modListCellFactory()
        modListView!!.items.setAll(modService.installedModVersions)
        try {
            modService.activatedSimAndUIMods.forEach { mod -> modListView!!.selectionModel.select(mod) }
        } catch (e: IOException) {
            logger.error("Activated mods could not be loaded", e)
        }

        modListView!!.scrollTo(modListView!!.selectionModel.selectedItem)
    }

    private fun initMapSelection() {
        filteredMapBeans = FilteredList(
                mapService.installedMaps.sorted { o1, o2 -> o1.displayName.compareTo(o2.displayName, ignoreCase = true) }
        )

        mapListView!!.setItems(filteredMapBeans)
        mapListView!!.setCellFactory { param -> StringListCell(Function<MapBean, String> { it.getDisplayName() }) }
        mapListView!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> Platform.runLater { setSelectedMap(newValue) } }
    }

    private fun setSelectedMap(newValue: MapBean?) {
        JavaFxUtil.assertApplicationThread()

        if (newValue == null) {
            mapNameLabel!!.text = ""
            return
        }

        preferencesService.preferences!!.lastMap = newValue.folderName
        preferencesService.storeInBackground()

        val largePreview = mapService.loadPreview(newValue.folderName, PreviewSize.LARGE)
        mapPreviewPane!!.background = Background(BackgroundImage(largePreview, NO_REPEAT, NO_REPEAT, CENTER,
                BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, false)))

        val mapSize = newValue.size
        mapSizeLabel!!.text = i18n.get("mapPreview.size", mapSize.widthInKm, mapSize.heightInKm)
        mapNameLabel!!.text = newValue.displayName
        mapPlayersLabel!!.text = i18n.number(newValue.players)
        mapDescriptionLabel!!.text = Optional.ofNullable(newValue.description)
                .map<String>(Function<String, String> { Strings.emptyToNull(it) })
                .map(Function<String, String> { obj, description -> obj.removeLocalizationTag(description) })
                .orElseGet({ i18n.get("map.noDescriptionAvailable") })

        val mapVersion = newValue.version
        if (mapVersion == null) {
            versionLabel!!.isVisible = false
        } else {
            versionLabel!!.text = i18n.get("map.versionFormat", mapVersion)
        }
    }

    private fun initFeaturedModList() {
        featuredModListView!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            preferencesService.preferences!!.lastGameType = newValue.technicalName
            preferencesService.storeInBackground()
        }
    }

    private fun initRatingBoundaries() {
        val lastGameMinRating = preferencesService.preferences!!.lastGameMinRating
        val lastGameMaxRating = preferencesService.preferences!!.lastGameMaxRating

        minRankingTextField!!.text = i18n.number(lastGameMinRating)
        maxRankingTextField!!.text = i18n.number(lastGameMaxRating)

        minRankingTextField!!.textProperty().addListener { observable, oldValue, newValue ->
            preferencesService.preferences!!.lastGameMinRating = Integer.parseInt(newValue)
            preferencesService.storeInBackground()
        }
        maxRankingTextField!!.textProperty().addListener { observable, oldValue, newValue ->
            preferencesService.preferences!!.lastGameMaxRating = Integer.parseInt(newValue)
            preferencesService.storeInBackground()
        }
    }

    private fun selectLastMap() {
        val lastMap = preferencesService.preferences!!.lastMap
        for (mapBean in mapListView!!.items) {
            if (mapBean.folderName.equals(lastMap, ignoreCase = true)) {
                mapListView!!.selectionModel.select(mapBean)
                mapListView!!.scrollTo(mapBean)
                return
            }
        }
        if (mapListView!!.selectionModel.isEmpty) {
            mapListView!!.selectionModel.selectFirst()
        }
    }

    private fun setLastGameTitle() {
        titleTextField!!.text = Strings.nullToEmpty(preferencesService.preferences!!.lastGameTitle)
    }

    private fun modListCellFactory(): Callback<ListView<ModVersion>, ListCell<ModVersion>> {
        return { param ->
            val cell = StringListCell<ModVersion>(Function<ModVersion, String> { it.getDisplayName() })
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
                modListView!!.requestFocus()
                val selectionModel = modListView!!.selectionModel
                if (!cell.isEmpty) {
                    val index = cell.index
                    if (selectionModel.selectedIndices.contains(index)) {
                        selectionModel.clearSelection(index)
                    } else {
                        selectionModel.select(index)
                    }
                    event.consume()
                }
            }
            cell
        }
    }

    private fun selectLastOrDefaultGameType() {
        var lastGameMod: String? = preferencesService.preferences!!.lastGameType
        if (lastGameMod == null) {
            lastGameMod = KnownFeaturedMod.DEFAULT.technicalName
        }

        for (mod in featuredModListView!!.items) {
            if (mod.technicalName == lastGameMod) {
                featuredModListView!!.selectionModel.select(mod)
                featuredModListView!!.scrollTo(mod)
                break
            }
        }
    }

    fun onRandomMapButtonClicked() {
        val mapIndex = (Math.random() * filteredMapBeans.size).toInt()
        mapListView!!.selectionModel.select(mapIndex)
        mapListView!!.scrollTo(mapIndex)
    }

    fun onCreateButtonClicked() {
        val selectedModVersions = modListView!!.selectionModel.selectedItems

        try {
            modService.overrideActivatedMods(modListView!!.selectionModel.selectedItems)
        } catch (e: IOException) {
            logger.warn("Activated mods could not be updated", e)
        }

        val simMods = selectedModVersions.stream()
                .map<String>(Function<ModVersion, String> { it.getUid() })
                .collect<Set<String>, Any>(Collectors.toSet())

        val newGameInfo = NewGameInfo(
                titleTextField!!.text,
                Strings.emptyToNull(passwordTextField!!.text),
                featuredModListView!!.selectionModel.selectedItem,
                mapListView!!.selectionModel.selectedItem.folderName,
                simMods,
                if (onlyForFriendsCheckBox!!.isSelected) GameVisibility.PRIVATE else GameVisibility.PUBLIC)

        gameService.hostGame(newGameInfo).exceptionally { throwable ->
            logger.warn("Game could not be hosted", throwable)
            notificationService.addNotification(
                    ImmediateErrorNotification(
                            i18n.get("errorTitle"),
                            i18n.get("game.create.failed"),
                            throwable,
                            i18n, reportingService
                    ))
            null
        }

        onCloseButtonClicked()
    }

    fun onSelectDefaultGameTypeButtonClicked() {
        featuredModListView!!.selectionModel.select(0)
    }

    fun onDeselectModsButtonClicked() {
        modListView!!.selectionModel.clearSelection()
    }

    fun onReloadModsButtonClicked() {
        modService.loadInstalledMods()
        initModList()
    }

    /**
     * @return returns true of the map was found and false if not
     */
    internal fun selectMap(mapFolderName: String): Boolean {
        val mapBeanOptional = mapListView!!.items.stream().filter { mapBean -> mapBean.folderName.equals(mapFolderName, ignoreCase = true) }.findAny()
        if (!mapBeanOptional.isPresent) {
            return false
        }
        mapListView!!.selectionModel.select(mapBeanOptional.get())
        mapListView!!.scrollTo(mapBeanOptional.get())
        return true
    }

    internal fun setOnCloseButtonClickedListener(onCloseButtonClickedListener: Runnable) {
        this.onCloseButtonClickedListener = onCloseButtonClickedListener
    }

    companion object {

        private val MAX_RATING_LENGTH = 4
        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
