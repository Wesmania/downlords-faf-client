package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.theme.UiService
import javafx.scene.control.TableCell
import javafx.scene.image.Image
import javafx.scene.image.ImageView

class MapPreviewTableCell(uiService: UiService) : TableCell<Game, Image>() {

    private val imageVew: ImageView

    init {
        val controller = uiService.loadFxml<Controller<ImageView>>("theme/vault/map/map_preview_table_cell.fxml")
        imageVew = controller.root
        graphic = imageVew
    }

    override fun updateItem(item: Image?, empty: Boolean) {
        super.updateItem(item, empty)

        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            imageVew.image = item
            graphic = imageVew
        }
    }
}

