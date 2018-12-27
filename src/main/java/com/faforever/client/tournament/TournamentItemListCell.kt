package com.faforever.client.tournament

import com.faforever.client.theme.UiService
import javafx.scene.control.ListCell

class TournamentItemListCell(uiService: UiService) : ListCell<TournamentBean>() {

    private val controller: TournamentListItemController

    init {
        controller = uiService.loadFxml("theme/tournaments/tournament_list_item.fxml")
        prefWidth = 0.0
    }

    override fun updateItem(item: TournamentBean?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) {
            text = null
            graphic = null
            return
        }

        controller.setTournamentItem(item)
        graphic = controller.root
    }
}
