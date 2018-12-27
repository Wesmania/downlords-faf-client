package com.faforever.client.util

import com.faforever.client.i18n.I18n
import com.faforever.client.preferences.ChatPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.preferences.TimeInfo
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.time.temporal.TemporalAccessor
import java.util.Locale
import java.util.TimeZone


@Lazy
@Service
class TimeService @Inject
constructor(private val i18n: I18n, private val preferencesService: PreferencesService) {

    private val currentTimeLocale: Locale
        get() {
            val chatPrefs = preferencesService.preferences!!.chat
            return if (chatPrefs.timeFormat == TimeInfo.AUTO) {
                Locale.getDefault()
            } else preferencesService.preferences!!.chat.timeFormat.usedLocale

        }

    /**
     * A string as "10 minutes ago"
     */
    fun timeAgo(temporal: Temporal?): String {
        if (temporal == null) {
            return ""
        }

        val ago = Duration.between(temporal, OffsetDateTime.now())

        if (Duration.ofMinutes(1).compareTo(ago) > 0) {
            return i18n.getQuantized("secondAgo", "secondsAgo", ago.seconds)
        }
        if (Duration.ofHours(1).compareTo(ago) > 0) {
            return i18n.getQuantized("minuteAgo", "minutesAgo", ago.toMinutes())
        }
        if (Duration.ofDays(1).compareTo(ago) > 0) {
            return i18n.getQuantized("hourAgo", "hoursAgo", ago.toHours())
        }
        if (Duration.ofDays(30).compareTo(ago) > 0) {
            return i18n.getQuantized("dayAgo", "daysAgo", ago.toDays())
        }
        return if (Duration.ofDays(365).compareTo(ago) > 0) {
            i18n.getQuantized("monthAgo", "monthsAgo", ago.toDays() / 30)
        } else i18n.getQuantized("yearAgo", "yearsAgo", ago.toDays() / 365)

    }

    /**
     * Returns [.timeAgo] if the specified instant is less than one day ago, otherwise a date string.
     */
    fun lessThanOneDayAgo(temporal: Temporal?): String {
        if (temporal == null) {
            return ""
        }

        val ago = Duration.between(temporal, OffsetDateTime.now())

        return if (ago.compareTo(Duration.ofDays(1)) <= 0) {
            timeAgo(temporal)
        } else asDate(temporal)

    }


    fun asDate(temporalAccessor: TemporalAccessor?): String {
        return if (temporalAccessor == null) {
            i18n.get("noDateAvailable")
        } else DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(currentTimeLocale)
                .withZone(TimeZone.getDefault().toZoneId())
                .format(temporalAccessor)
    }


    fun asShortTime(temporal: Temporal): String {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(currentTimeLocale)
                .format(ZonedDateTime.ofInstant(Instant.from(temporal), TimeZone.getDefault().toZoneId()))
    }


    fun asIsoTime(temporal: Temporal): String {
        return DateTimeFormatter.ISO_TIME.format(temporal)
    }

    /**
     * Returns the localized minutes and seconds (e.g. '20min 31s'), or hours and minutes (e.g. '1h 5min') of the
     * specified duration.
     */
    fun shortDuration(duration: Duration?): String {
        if (duration == null) {
            return ""
        }

        if (Duration.ofMinutes(1).compareTo(duration) > 0) {
            return i18n.get("duration.seconds", duration.seconds)
        }
        return if (Duration.ofHours(1).compareTo(duration) > 0) {
            i18n.get("duration.minutesSeconds", duration.toMinutes(), duration.seconds % 60)
        } else i18n.get("duration.hourMinutes", duration.toMinutes() / 60, duration.toMinutes() % 60)

    }

    /**
     * Returns e.g. "3:21:12" (h:mm:ss).
     */
    fun asHms(duration: Duration): String {
        val seconds = duration.seconds
        return String.format("%d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60)
    }
}
