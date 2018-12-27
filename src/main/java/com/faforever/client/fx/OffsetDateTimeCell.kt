package com.faforever.client.fx

import com.faforever.client.util.TimeService
import javafx.scene.control.TableCell

import java.time.OffsetDateTime

class OffsetDateTimeCell<S>(private val timeService: TimeService) : TableCell<S, OffsetDateTime>() {

    override fun updateItem(item: OffsetDateTime, empty: Boolean) {
        super.updateItem(item, empty)
        if (!empty) {
            text = timeService.asDate(item)
        }
    }
}
