package com.faforever.client.fx

interface Controller<ROOT> {

    val root: ROOT

    /** Magic method called by JavaFX after FXML has been loaded.  */
    open fun initialize() {

    }
}
