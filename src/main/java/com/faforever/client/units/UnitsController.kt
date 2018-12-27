package com.faforever.client.units

import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.UnitDatabase
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.preferences.Preferences.UnitDataBaseType
import com.faforever.client.preferences.PreferencesService
import com.google.common.base.Strings
import javafx.scene.Node
import javafx.scene.web.WebView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class UnitsController @Inject
constructor(private val clientProperties: ClientProperties, private val preferencesService: PreferencesService, private val cookieService: CookieService) : AbstractViewController<Node>() {
    var unitsRoot: WebView? = null

    override val root: Node?
        get() = unitsRoot

    override fun onDisplay(navigateEvent: NavigateEvent) {
        if (Strings.isNullOrEmpty(unitsRoot!!.engine.location)) {
            cookieService.setUpCookieManger()
            loadUnitDataBase(preferencesService.preferences!!.unitDataBaseType)
            JavaFxUtil.addListener(preferencesService.preferences!!.unitDataBaseTypeProperty()) { observable, oldValue, newValue -> loadUnitDataBase(newValue) }
        }
    }

    private fun loadUnitDataBase(newValue: UnitDataBaseType) {
        val unitDatabase = clientProperties.getUnitDatabase()
        unitsRoot!!.engine.load(if (newValue == UnitDataBaseType.SPOOKY) unitDatabase.getSpookiesUrl() else unitDatabase.getRackOversUrl())
    }

}
