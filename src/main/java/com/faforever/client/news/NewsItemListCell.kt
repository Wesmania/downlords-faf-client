package com.faforever.client.news

import com.faforever.client.theme.UiService
import javafx.scene.control.ListCell

class NewsItemListCell(uiService: UiService) : ListCell<NewsItem>() {

    private val controller: NewsListItemController

    init {
        controller = uiService.loadFxml("theme/news_list_item.fxml")
        prefWidth = 0.0
    }

    override fun updateItem(item: NewsItem?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) {
            text = null
            graphic = null
            return
        }

        controller.setNewsItem(item)
        graphic = controller.root
    }
}
