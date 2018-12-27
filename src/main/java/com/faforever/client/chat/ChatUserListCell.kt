package com.faforever.client.chat

import com.faforever.client.theme.UiService
import javafx.scene.control.ListCell

import java.util.Objects

class ChatUserListCell(uiService: UiService) : ListCell<CategoryOrChatUserListItem>() {

    private val chatUserItemController: ChatUserItemController
    private val chatUserCategoryController: ChatUserItemCategoryController
    private var oldItem: Any? = null

    init {
        chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml")
        chatUserCategoryController = uiService.loadFxml("theme/chat/chat_user_category.fxml")

        text = null
    }

    override fun updateItem(item: CategoryOrChatUserListItem?, empty: Boolean) {
        if (item == oldItem) {
            return
        }
        oldItem = item

        super.updateItem(item, empty)

        if (item == null || empty) {
            chatUserItemController.chatUser = null
            chatUserCategoryController.setChatUserCategory(null)
            graphic = null
            return
        }

        if (item.getUser() != null) {
            chatUserCategoryController.setChatUserCategory(null)
            chatUserItemController.chatUser = item.getUser()
            graphic = chatUserItemController.root
        } else {
            chatUserItemController.chatUser = null
            chatUserCategoryController.setChatUserCategory(item.getCategory())
            graphic = chatUserCategoryController.root
        }
    }
}
