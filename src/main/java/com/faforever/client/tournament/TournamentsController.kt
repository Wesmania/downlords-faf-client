package com.faforever.client.tournament


import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.WebViewConfigurer
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import com.faforever.client.util.TimeService
import com.google.common.io.CharStreams
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.layout.Pane
import javafx.scene.web.WebView
import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

import java.io.InputStreamReader
import java.io.Reader
import java.text.MessageFormat
import java.util.Comparator

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class TournamentsController(private val timeService: TimeService, private val i18n: I18n, private val tournamentService: TournamentService, private val uiService: UiService, private val webViewConfigurer: WebViewConfigurer, private val preferencesService: PreferencesService) : AbstractViewController<Node>() {

    var tournamentRoot: Pane? = null
    var tournamentDetailWebView: WebView? = null
    var loadingIndicator: Pane? = null
    var contentPane: Node? = null
    var tournamentListView: ListView<TournamentBean>? = null

    override val root: Node?
        get() = tournamentRoot

    override fun initialize() {
        contentPane!!.managedProperty().bind(contentPane!!.visibleProperty())
        contentPane!!.isVisible = false

        tournamentListView!!.setCellFactory { param -> TournamentItemListCell(uiService) }
        tournamentListView!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> displayTournamentItem(newValue) }
    }

    private fun onLoadingStart() {
        Platform.runLater { loadingIndicator!!.isVisible = true }
    }

    private fun onLoadingStop() {
        JavaFxUtil.runLater {
            tournamentRoot!!.children.remove(loadingIndicator)
            loadingIndicator = null
            contentPane!!.isVisible = true
        }
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (contentPane!!.isVisible) {
            return
        }
        onLoadingStart()

        tournamentDetailWebView!!.isContextMenuEnabled = false
        webViewConfigurer.configureWebView(tournamentDetailWebView)

        tournamentService.allTournaments
                .thenAccept { tournaments ->
                    Platform.runLater {
                        tournaments.sort(Comparator.comparing<TournamentBean, OffsetDateTime>(Function<TournamentBean, OffsetDateTime> { it.getCreatedAt() }))
                        tournamentListView!!.items.setAll(tournaments)
                        tournamentListView!!.selectionModel.selectFirst()
                        onLoadingStop()
                    }
                }.exceptionally { throwable ->
                    log.warn("Tournaments could not be loaded", throwable)
                    null
                }
    }

    @SneakyThrows
    private fun displayTournamentItem(tournamentBean: TournamentBean) {
        var startingDate = i18n.get("tournament.noStartingDate")
        if (tournamentBean.startingAt != null) {
            startingDate = MessageFormat.format(i18n.get("dateWithTime"), timeService.asDate(tournamentBean.startingAt), timeService.asShortTime(tournamentBean.startingAt))
        }

        var completedDate = i18n.get("tournament.noCompletionDate")
        if (tournamentBean.completedAt != null) {
            completedDate = MessageFormat.format(i18n.get("dateWithTime"), timeService.asDate(tournamentBean.completedAt), timeService.asShortTime(tournamentBean.completedAt))
        }

        val reader = InputStreamReader(TOURNAMENT_DETAIL_HTML_RESOURCE.inputStream)
        val html = CharStreams.toString(reader).replace("{name}", tournamentBean.name)
                .replace("{challonge-url}", tournamentBean.challongeUrl)
                .replace("{tournament-type}", tournamentBean.tournamentType)
                .replace("{starting-date}", startingDate)
                .replace("{completed-date}", completedDate)
                .replace("{description}", tournamentBean.description)
                .replace("{tournament-image}", tournamentBean.liveImageUrl)
                .replace("{open-on-challonge-label}", i18n.get("tournament.openOnChallonge"))
                .replace("{game-type-label}", i18n.get("tournament.gameType"))
                .replace("{starting-at-label}", i18n.get("tournament.startingAt"))
                .replace("{completed-at-label}", i18n.get("tournament.completedAt"))
                .replace("{loading-label}", i18n.get("loading"))

        tournamentDetailWebView!!.engine.loadContent(html)
    }

    companion object {
        private val TOURNAMENT_DETAIL_HTML_RESOURCE = ClassPathResource("/theme/tournaments/tournament_detail.html")
    }
}
