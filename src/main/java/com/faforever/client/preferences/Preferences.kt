package com.faforever.client.preferences

import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder
import com.faforever.client.game.KnownFeaturedMod
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ListProperty
import javafx.beans.property.MapProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.beans.property.SimpleMapProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.control.TableColumn.SortType
import javafx.util.Pair
import lombok.Getter

import java.net.HttpCookie
import java.net.URI
import java.util.ArrayList

import javafx.collections.FXCollections.observableArrayList

class Preferences {

    val mainWindow: WindowPrefs
    val forgedAlliance: ForgedAlliancePrefs
    val login: LoginPrefs
    val chat: ChatPrefs
    val notification: NotificationsPrefs
    private val themeName: StringProperty
    private val lastGameType: StringProperty
    val localization: LocalizationPrefs
    private val lastGameTitle: StringProperty
    private val lastMap: StringProperty
    private val rememberLastTab: BooleanProperty
    private val showPasswordProtectedGames: BooleanProperty
    private val showModdedGames: BooleanProperty
    private val ignoredNotifications: ListProperty<String>
    private val lastGameMinRating: IntegerProperty
    private val lastGameMaxRating: IntegerProperty
    private val gamesViewMode: StringProperty
    val ladder1v1Prefs: Ladder1v1Prefs
    val news: NewsPrefs
    val developer: DeveloperPrefs
    val vaultPrefs: VaultPrefs
    private val gameListSorting: ListProperty<Pair<String, SortType>>
    private val gameTileSortingOrder: ObjectProperty<TilesSortingOrder>
    private val unitDataBaseType: ObjectProperty<UnitDataBaseType>
    private val storedCookies: MapProperty<URI, ArrayList<HttpCookie>>
    private val lastGameOnlyFriends: BooleanProperty

    var isLastGameOnlyFriends: Boolean
        get() = lastGameOnlyFriends.get()
        set(lastGameOnlyFriends) = this.lastGameOnlyFriends.set(lastGameOnlyFriends)

    init {
        gameTileSortingOrder = SimpleObjectProperty(TilesSortingOrder.PLAYER_DES)
        chat = ChatPrefs()
        login = LoginPrefs()

        localization = LocalizationPrefs()
        mainWindow = WindowPrefs()
        forgedAlliance = ForgedAlliancePrefs()
        themeName = SimpleStringProperty(DEFAULT_THEME_NAME)
        lastGameType = SimpleStringProperty(KnownFeaturedMod.DEFAULT.technicalName)
        ignoredNotifications = SimpleListProperty(observableArrayList())
        notification = NotificationsPrefs()
        rememberLastTab = SimpleBooleanProperty(true)
        lastGameTitle = SimpleStringProperty()
        lastMap = SimpleStringProperty()
        lastGameMinRating = SimpleIntegerProperty(800)
        lastGameMaxRating = SimpleIntegerProperty(1300)
        ladder1v1Prefs = Ladder1v1Prefs()
        gamesViewMode = SimpleStringProperty()
        news = NewsPrefs()
        developer = DeveloperPrefs()
        gameListSorting = SimpleListProperty(observableArrayList())
        vaultPrefs = VaultPrefs()
        unitDataBaseType = SimpleObjectProperty(UnitDataBaseType.RACKOVER)
        storedCookies = SimpleMapProperty(FXCollections.observableHashMap())
        showPasswordProtectedGames = SimpleBooleanProperty(true)
        showModdedGames = SimpleBooleanProperty(true)
        lastGameOnlyFriends = SimpleBooleanProperty()
    }


    fun getGameTileSortingOrder(): TilesSortingOrder {
        return gameTileSortingOrder.get()
    }

    fun setGameTileSortingOrder(gameTileTilesSortingOrder: TilesSortingOrder) {
        this.gameTileSortingOrder.set(gameTileTilesSortingOrder)
    }

    fun gameTileSortingOrderProperty(): ObjectProperty<TilesSortingOrder> {
        return gameTileSortingOrder
    }

    fun showPasswordProtectedGamesProperty(): BooleanProperty {
        return showPasswordProtectedGames
    }

    fun showModdedGamesProperty(): BooleanProperty {
        return showModdedGames
    }

    fun getGamesViewMode(): String {
        return gamesViewMode.get()
    }

    fun setGamesViewMode(gamesViewMode: String) {
        this.gamesViewMode.set(gamesViewMode)
    }

    fun gamesViewModeProperty(): StringProperty {
        return gamesViewMode
    }

    fun getThemeName(): String {
        return themeName.get()
    }

    fun setThemeName(themeName: String) {
        this.themeName.set(themeName)
    }

    fun themeNameProperty(): StringProperty {
        return themeName
    }

    fun getLastGameType(): String {
        return lastGameType.get()
    }

    fun setLastGameType(lastGameType: String) {
        this.lastGameType.set(lastGameType)
    }

    fun lastGameTypeProperty(): StringProperty {
        return lastGameType
    }

    fun getLastGameTitle(): String {
        return lastGameTitle.get()
    }

    fun setLastGameTitle(lastGameTitle: String) {
        this.lastGameTitle.set(lastGameTitle)
    }

    fun lastGameTitleProperty(): StringProperty {
        return lastGameTitle
    }

    fun getLastMap(): String {
        return lastMap.get()
    }

    fun setLastMap(lastMap: String) {
        this.lastMap.set(lastMap)
    }

    fun lastMapProperty(): StringProperty {
        return lastMap
    }

    fun getRememberLastTab(): Boolean {
        return rememberLastTab.get()
    }

    fun setRememberLastTab(rememberLastTab: Boolean) {
        this.rememberLastTab.set(rememberLastTab)
    }

    fun rememberLastTabProperty(): BooleanProperty {
        return rememberLastTab
    }

    fun getIgnoredNotifications(): ObservableList<String> {
        return ignoredNotifications.get()
    }

    fun setIgnoredNotifications(ignoredNotifications: ObservableList<String>) {
        this.ignoredNotifications.set(ignoredNotifications)
    }

    fun ignoredNotificationsProperty(): ListProperty<String> {
        return ignoredNotifications
    }

    fun getLastGameMinRating(): Int {
        return lastGameMinRating.get()
    }

    fun setLastGameMinRating(lastGameMinRating: Int) {
        this.lastGameMinRating.set(lastGameMinRating)
    }

    fun lastGameMinRatingProperty(): IntegerProperty {
        return lastGameMinRating
    }

    fun getLastGameMaxRating(): Int {
        return lastGameMaxRating.get()
    }

    fun setLastGameMaxRating(lastGameMaxRating: Int) {
        this.lastGameMaxRating.set(lastGameMaxRating)
    }

    fun lastGameMaxRatingProperty(): IntegerProperty {
        return lastGameMaxRating
    }

    fun getGameListSorting(): ObservableList<Pair<String, SortType>> {
        return gameListSorting.get()
    }

    fun getUnitDataBaseType(): UnitDataBaseType {
        return unitDataBaseType.get()
    }

    fun setUnitDataBaseType(unitDataBaseType: UnitDataBaseType) {
        this.unitDataBaseType.set(unitDataBaseType)
    }

    fun unitDataBaseTypeProperty(): ObjectProperty<UnitDataBaseType> {
        return unitDataBaseType
    }

    fun getStoredCookies(): ObservableMap<URI, ArrayList<HttpCookie>> {
        return storedCookies.get()
    }

    fun lastGameOnlyFriendsProperty(): BooleanProperty {
        return lastGameOnlyFriends
    }

    enum class UnitDataBaseType private constructor(@field:Getter
                                                    private val i18nKey: String) {
        SPOOKY("unitDatabase.spooky"),
        RACKOVER("unitDatabase.rackover")
    }

    companion object {

        val DEFAULT_THEME_NAME = "default"
    }
}
