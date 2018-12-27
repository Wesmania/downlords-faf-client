package com.faforever.client.game

import com.faforever.client.chat.CountryFlagService
import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject


@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PlayerCardTooltipController @Inject
constructor(private val countryFlagService: CountryFlagService, private val i18n: I18n) : Controller<Node> {
    var playerInfo: Label? = null
    var countryImageView: ImageView? = null

    override val root: Node?
        get() = playerInfo

    fun setPlayer(player: Player?, rating: Int) {
        if (player == null) {
            return
        }
        countryFlagService.loadCountryFlag(player.country).ifPresent { image -> countryImageView!!.image = image }

        val playerInfoLocalized = i18n.get("userInfo.tooltipFormat", player.username, rating)
        playerInfo!!.text = playerInfoLocalized
    }
}
