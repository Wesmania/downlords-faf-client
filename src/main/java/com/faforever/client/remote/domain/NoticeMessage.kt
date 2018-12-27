package com.faforever.client.remote.domain

import com.faforever.client.notification.Severity

class NoticeMessage : FafServerMessage(FafServerMessageType.NOTICE) {

    var text: String? = null
    private var style: String? = null

    val severity: Severity
        get() {
            if (style == null) {
                return Severity.INFO
            }
            when (style) {
                "error" -> return Severity.ERROR
                "warning" -> return Severity.WARN
                "info" -> return Severity.INFO
                else -> return Severity.INFO
            }
        }

    fun setStyle(style: String) {
        this.style = style
    }
}
