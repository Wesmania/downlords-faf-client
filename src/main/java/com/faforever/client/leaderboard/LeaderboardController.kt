package com.faforever.client.leaderboard

import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.StringCell
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.util.Assert
import com.faforever.client.util.Validator
import javafx.beans.property.SimpleFloatProperty
import javafx.scene.Node
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles

import javafx.collections.FXCollections.observableList


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class LeaderboardController @Inject
constructor(private val leaderboardService: LeaderboardService, private val notificationService: NotificationService, private val i18n: I18n, private val reportingService: ReportingService) : AbstractViewController<Node>() {
    var leaderboardRoot: Pane? = null
    var rankColumn: TableColumn<LeaderboardEntry, Number>? = null
    var nameColumn: TableColumn<LeaderboardEntry, String>? = null
    var winLossColumn: TableColumn<LeaderboardEntry, Number>? = null
    var gamesPlayedColumn: TableColumn<LeaderboardEntry, Number>? = null
    var ratingColumn: TableColumn<LeaderboardEntry, Number>? = null
    var ratingTable: TableView<LeaderboardEntry>? = null
    var searchTextField: TextField? = null
    var connectionProgressPane: Pane? = null
    var contentPane: Pane? = null
    private var ratingType: KnownFeaturedMod? = null

    override val root: Node?
        get() = leaderboardRoot

    override fun initialize() {
        super.initialize()
        rankColumn!!.setCellValueFactory { param -> param.value.rankProperty() }
        rankColumn!!.setCellFactory { param -> StringCell { rank -> i18n.number(rank.toInt()) } }

        nameColumn!!.setCellValueFactory { param -> param.value.usernameProperty() }
        nameColumn!!.setCellFactory { param -> StringCell { name -> name } }

        winLossColumn!!.setCellValueFactory { param -> SimpleFloatProperty(param.value.winLossRatio) }
        winLossColumn!!.setCellFactory { param -> StringCell { number -> i18n.get("percentage", number.toFloat() * 100) } }

        gamesPlayedColumn!!.setCellValueFactory { param -> param.value.gamesPlayedProperty() }
        gamesPlayedColumn!!.setCellFactory { param -> StringCell { count -> i18n.number(count.toInt()) } }

        ratingColumn!!.setCellValueFactory { param -> param.value.ratingProperty() }
        ratingColumn!!.setCellFactory { param -> StringCell { rating -> i18n.number(rating.toInt()) } }

        contentPane!!.managedProperty().bind(contentPane!!.visibleProperty())
        connectionProgressPane!!.managedProperty().bind(connectionProgressPane!!.visibleProperty())
        connectionProgressPane!!.visibleProperty().bind(contentPane!!.visibleProperty().not())

        searchTextField!!.textProperty().addListener { observable, oldValue, newValue ->
            if (Validator.isInt(newValue)) {
                ratingTable!!.scrollTo(Integer.parseInt(newValue) - 1)
            } else {
                var foundPlayer: LeaderboardEntry? = null
                for (leaderboardEntry in ratingTable!!.items) {
                    if (leaderboardEntry.username.toLowerCase().startsWith(newValue.toLowerCase())) {
                        foundPlayer = leaderboardEntry
                        break
                    }
                }
                if (foundPlayer == null) {
                    for (leaderboardEntry in ratingTable!!.items) {
                        if (leaderboardEntry.username.toLowerCase().contains(newValue.toLowerCase())) {
                            foundPlayer = leaderboardEntry
                            break
                        }
                    }
                }
                if (foundPlayer != null) {
                    ratingTable!!.scrollTo(foundPlayer)
                    ratingTable!!.selectionModel.select(foundPlayer)
                } else {
                    ratingTable!!.selectionModel.select(null)
                }
            }
        }
    }

    override fun onDisplay(navigateEvent: NavigateEvent) {
        Assert.checkNullIllegalState(ratingType, "ratingType must not be null")

        contentPane!!.isVisible = false
        leaderboardService.getEntries(ratingType).thenAccept { leaderboardEntryBeans ->
            ratingTable!!.items = observableList(leaderboardEntryBeans)
            contentPane!!.isVisible = true
        }.exceptionally { throwable ->
            contentPane!!.isVisible = false
            logger.warn("Error while loading leaderboard entries", throwable)
            notificationService.addNotification(ImmediateErrorNotification(
                    i18n.get("errorTitle"), i18n.get("leaderboard.failedToLoad"),
                    throwable, i18n, reportingService
            ))
            null
        }
    }

    fun setRatingType(ratingType: KnownFeaturedMod) {
        this.ratingType = ratingType
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
