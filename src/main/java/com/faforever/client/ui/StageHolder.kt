package com.faforever.client.ui

import javafx.stage.Stage
import org.springframework.util.Assert

object StageHolder {
    var stage: Stage? = null
        get() {
            Assert.state(field != null, "Stage has not yet been set")
            return field
        }
}// Static class
