package com.faforever.client.clan


import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Optional

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ClanTooltipController(private val i18n: I18n) : Controller<Node> {

    override var root: Pane? = null
    var nameLabel: Label? = null
    var descriptionLabel: Label? = null
    var membersLabel: Label? = null
    var leaderLabel: Label? = null
    var descriptionTitleLabel: Label? = null

    override fun initialize() {
        descriptionLabel!!.managedProperty().bind(descriptionLabel!!.visibleProperty())
        descriptionTitleLabel!!.managedProperty().bind(descriptionTitleLabel!!.visibleProperty())
        descriptionLabel!!.visibleProperty().bind(descriptionLabel!!.textProperty().isNotEmpty)
        descriptionTitleLabel!!.visibleProperty().bind(descriptionLabel!!.textProperty().isNotEmpty)
    }

    fun setClan(clan: Clan) {
        nameLabel!!.text = clan.name
        // TODO improve formatting
        membersLabel!!.text = i18n.number(clan.members.size)
        descriptionLabel!!.text = clan.description
        leaderLabel!!.text = clan.leader.username
        descriptionLabel!!.text = Optional.ofNullable(clan.description).orElse("")
    }
}
