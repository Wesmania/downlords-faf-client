package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapBean
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.mod.ModService
import com.faforever.client.theme.UiService
import com.google.common.base.Joiner
import javafx.application.Platform
import javafx.beans.binding.StringBinding
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import javax.inject.Inject
import kotlin.collections.Map.Entry
import java.util.Objects
import java.util.function.Consumer
import java.util.stream.Collectors

import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.binding.Bindings.createStringBinding

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class GameTileController @Inject
constructor(private val mapService: MapService, private val i18n: I18n, private val joinGameHelper: JoinGameHelper, private val modService: ModService, private val uiService: UiService) : Controller<Node> {
    var lockIconLabel: Label? = null
    var gameTypeLabel: Label? = null
    override var root: Node? = null
    var gameMapLabel: Label? = null
    var gameTitleLabel: Label? = null
    var numberOfPlayersLabel: Label? = null
    var hostLabel: Label? = null
    var modsLabel: Label? = null
    var mapImageView: ImageView? = null
    private var onSelectedListener: Consumer<Game>? = null
    private var game: Game? = null
    private var tooltip: Tooltip? = null

    fun setOnSelectedListener(onSelectedListener: Consumer<Game>) {
        this.onSelectedListener = onSelectedListener
    }

    override fun initialize() {
        modsLabel!!.managedProperty().bind(modsLabel!!.visibleProperty())
        modsLabel!!.visibleProperty().bind(modsLabel!!.textProperty().isNotEmpty)
        gameTypeLabel!!.managedProperty().bind(gameTypeLabel!!.visibleProperty())
        lockIconLabel!!.managedProperty().bind(lockIconLabel!!.visibleProperty())
    }

    fun setGame(game: Game) {
        Assert.isNull(this.game, "Game has already been set")
        this.game = game

        modService.getFeaturedMod(game.featuredMod)
                .thenAccept { featuredModBean -> Platform.runLater { gameTypeLabel!!.text = StringUtils.defaultString(featuredModBean.displayName) } }

        gameTitleLabel!!.textProperty().bind(game.titleProperty())
        hostLabel!!.text = game.host

        val mapNameBinding = createStringBinding(
                {
                    mapService.getMapLocallyFromName(game.mapFolderName)
                            .map<String>(Function<MapBean, String> { it.getDisplayName() })
                            .orElse(game.mapFolderName)
                },
                game.mapFolderNameProperty())

        JavaFxUtil.bind(gameMapLabel!!.textProperty(), mapNameBinding)
        numberOfPlayersLabel!!.textProperty().bind(createStringBinding(
                { i18n.get("game.players.format", game.numPlayers, game.maxPlayers) },
                game.numPlayersProperty(),
                game.maxPlayersProperty()
        ))
        mapImageView!!.imageProperty().bind(createObjectBinding<Image>(
                { mapService.loadPreview(game.mapFolderName, PreviewSize.SMALL) },
                game.mapFolderNameProperty()
        ))

        val simMods = game.simMods
        modsLabel!!.textProperty().bind(createStringBinding({ getSimModsLabelContent(simMods) }, simMods))

        // TODO display "unknown map" image first since loading may take a while
        mapImageView!!.imageProperty().bind(createObjectBinding<Image>(
                { mapService.loadPreview(game.mapFolderName, PreviewSize.SMALL) },
                game.mapFolderNameProperty()
        ))

        lockIconLabel!!.visibleProperty().bind(game.passwordProtectedProperty())
    }

    private fun getSimModsLabelContent(simMods: ObservableMap<String, String>): String {
        val modNames = simMods.entries.stream()
                .limit(2)
                .map<String>(Function<Entry<String, String>, String> { it.value })
                .collect<List<String>, Any>(Collectors.toList())

        return if (simMods.size > 2) {
            i18n.get("game.mods.twoAndMore", modNames[0], modNames.size)
        } else Joiner.on(i18n.get("textSeparator")).join(modNames)
    }

    fun onClick(mouseEvent: MouseEvent) {
        Objects.requireNonNull<Consumer<Game>>(onSelectedListener, "onSelectedListener has not been set")
        Objects.requireNonNull<Game>(game, "gameInfoBean has not been set")

        root!!.requestFocus()
        onSelectedListener!!.accept(game)

        if (mouseEvent.button == MouseButton.PRIMARY && mouseEvent.clickCount == 2) {
            mouseEvent.consume()
            joinGameHelper.join(game)
        }
    }

    fun onMouseEntered() {
        val gameTooltipController = uiService.loadFxml<GameTooltipController>("theme/play/game_tooltip.fxml")
        gameTooltipController.setGame(game)

        tooltip = Tooltip()
        tooltip!!.graphic = gameTooltipController.root
        Tooltip.install(root, tooltip)
    }

    fun onMouseExited() {
        Tooltip.uninstall(root, tooltip)
        tooltip = null
    }
}
