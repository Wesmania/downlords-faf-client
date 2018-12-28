package com.faforever.client.i18n

import com.faforever.client.preferences.PreferencesService
import com.google.common.base.Strings
import javafx.beans.property.ReadOnlySetWrapper
import javafx.collections.FXCollections
import javafx.collections.ObservableSet
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

@Service
class I18n(private val messageSource: ReloadableResourceBundleMessageSource, private val preferencesService: PreferencesService) {
    private val availableLanguages: ObservableSet<Locale> = FXCollections.observableSet(HashSet())

    private var userSpecificLocale: Locale? = null

    @PostConstruct
    @Throws(IOException::class)
    fun postConstruct() {
        val locale = preferencesService.preferences!!.localization.language
        if (locale != null) {
            userSpecificLocale = Locale(locale.language, locale.country)
        } else {
            userSpecificLocale = Locale.getDefault()
        }

        loadAvailableLanguages()
    }

    @Throws(IOException::class)
    private fun loadAvailableLanguages() {
        // These are the default languages shipped with the client
        availableLanguages.addAll(Set.of(
                Locale.US,
                Locale("cs"),
                Locale.GERMAN,
                Locale.FRENCH,
                Locale("ru"),
                Locale.CHINESE
        ))

        val languagesDirectory = preferencesService.languagesDirectory
        if (Files.notExists(languagesDirectory)) {
            return
        }

        val currentBaseNames = messageSource.basenameSet
        val newBaseNames = LinkedHashSet<String>()
        Files.list(languagesDirectory).use { dir ->
            dir
                    .map { path -> MESSAGES_FILE_PATTERN.matcher(path.toString()) }
                    .filter { it.matches() }
                    .forEach { matcher ->
                        newBaseNames.add(Paths.get(matcher.group(1)).toUri().toString())
                        availableLanguages.add(Locale(matcher.group(2), Strings.nullToEmpty(matcher.group(3))))
                    }
        }
        // Make sure that current base names are added last; the files above have precedence
        newBaseNames.addAll(currentBaseNames)
        messageSource.setBasenames(*newBaseNames.toTypedArray())
    }

    operator fun get(key: String, vararg args: Any): String {
        return get(userSpecificLocale, key, *args)
    }

    operator fun get(locale: Locale?, key: String, vararg args: Any): String {
        return messageSource.getMessage(key, args, locale!!)
    }

    fun getQuantized(singularKey: String, pluralKey: String, arg: Long): String {
        val args = arrayOf<Any>(arg)
        return if (Math.abs(arg) == 1L) {
            messageSource.getMessage(singularKey, args, userSpecificLocale!!)
        } else messageSource.getMessage(pluralKey, args, userSpecificLocale!!)
    }

    fun number(number: Int): String {
        return String.format(userSpecificLocale!!, "%d", number)
    }

    fun numberWithSign(number: Int): String {
        return String.format(userSpecificLocale!!, "%+d", number)
    }

    fun rounded(number: Double, digits: Int): String {
        return String.format(userSpecificLocale!!, "%." + digits + "f", number)
    }

    fun getAvailableLanguages(): ReadOnlySetWrapper<Locale> {
        return ReadOnlySetWrapper(availableLanguages)
    }

    companion object {
        private val MESSAGES_FILE_PATTERN = Pattern.compile("(.*[/\\\\]messages)(?:_([a-z]{2}))(?:_([a-z]{2}))?\\.properties", Pattern.CASE_INSENSITIVE)
    }
}
