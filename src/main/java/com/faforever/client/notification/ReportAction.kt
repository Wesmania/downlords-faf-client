package com.faforever.client.notification

import com.faforever.client.i18n.I18n
import com.faforever.client.reporting.ReportingService

class ReportAction(i18n: I18n, reportingService: ReportingService, throwable: Throwable) : Action(i18n.get("report"), { event -> reportingService.reportError(throwable) })
