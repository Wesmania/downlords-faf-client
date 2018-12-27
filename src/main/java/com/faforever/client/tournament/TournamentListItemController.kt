package com.faforever.client.tournament

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.util.TimeService
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.text.MessageFormat
import java.time.OffsetDateTime

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TournamentListItemController(private val i18n: I18n, private val timeService: TimeService) : Controller<Node> {
    override var root: Node? = null
    var imageView: ImageView? = null
    var nameLabel: Label? = null
    var statusLabel: Label? = null
    var startingLabel: Label? = null

    internal fun setTournamentItem(tournamentBean: TournamentBean) {

        // TODO only use this if there's no thumbnail. However, there's never a thumbnail ATM.
        //    imageView.setImage(uiService.getThemeImage(tournamentBean.getNewsCategory().getImagePath()));

        nameLabel!!.text = tournamentBean.name
        if (tournamentBean.startingAt == null) {
            startingLabel!!.text = i18n.get("unknown")
        } else {
            startingLabel!!.text = MessageFormat.format("{0} {1}", timeService.asDate(tournamentBean.startingAt), timeService.asShortTime(tournamentBean.startingAt))
        }

        val statusKey: String
        if (tournamentBean.completedAt != null) {
            statusKey = "tournament.status.finished"
        } else if (tournamentBean.startingAt != null && tournamentBean.startingAt.isBefore(OffsetDateTime.now())) {
            statusKey = "tournament.status.running"
        } else if (tournamentBean.isOpenForSignup) {
            statusKey = "tournament.status.openForRegistration"
        } else {
            statusKey = "tournament.status.closedForRegistration"
        }

        statusLabel!!.text = i18n.get(statusKey)
    }
}
