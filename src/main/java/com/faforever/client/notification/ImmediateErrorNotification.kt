package com.faforever.client.notification

import com.faforever.client.i18n.I18n
import com.faforever.client.reporting.ReportingService

import java.util.Arrays

class ImmediateErrorNotification(title: String, text: String, throwable: Throwable, i18n: I18n, reportingService: ReportingService) : ImmediateNotification(title, text, Severity.ERROR, throwable, Arrays.asList(
        ReportAction(i18n, reportingService, throwable),
        DismissAction(i18n)
))
