package com.faforever.client.settings

import com.faforever.client.chat.CountryFlagService
import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.google.common.base.Strings
import com.jfoenix.controls.JFXRippler
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Locale
import java.util.Optional
import java.util.function.Consumer

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class LanguageItemController(private val i18n: I18n, private val countryFlagService: CountryFlagService) : Controller<Node> {
    var languageItemRoot: Pane? = null
    var localLanguageLabel: Label? = null
    var translatedLanguageLabel: Label? = null
    var localeImageView: ImageView? = null
    var checkedLabel: Label? = null
    private var listener: Consumer<Locale>? = null
    private var locale: Locale? = null
    private var jfxRippler: JFXRippler? = null

    override val root: Node?
        get() = jfxRippler

    override fun initialize() {
        checkedLabel!!.managedProperty().bind(checkedLabel!!.visibleProperty())

        localeImageView!!.managedProperty().bind(localeImageView!!.visibleProperty())
        localeImageView!!.isVisible = false
        jfxRippler = JFXRippler(languageItemRoot)
    }

    fun setLocale(locale: Locale) {
        this.locale = locale
        localLanguageLabel!!.text = locale.getDisplayName(locale)
        translatedLanguageLabel!!.text = locale.getDisplayName(i18n.userSpecificLocale)

        Optional.ofNullable(Strings.emptyToNull(locale.country))
                .flatMap<Image>(Function<String, Optional<Image>> { countryFlagService.loadCountryFlag(it) })
                .ifPresent { image ->
                    localeImageView!!.image = image
                    localeImageView!!.isVisible = true
                }
    }

    fun setOnSelectedListener(listener: Consumer<Locale>) {
        this.listener = listener
    }

    fun setSelected(selected: Boolean) {
        checkedLabel!!.isVisible = selected
    }

    fun onSelected() {
        listener!!.accept(locale)
    }
}
