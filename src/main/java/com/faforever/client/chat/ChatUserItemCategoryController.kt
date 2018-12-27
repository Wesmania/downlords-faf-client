package com.faforever.client.chat

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import javafx.scene.Node
import javafx.scene.control.Label
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/** Represents a header in the chat user list, like "Moderators".  */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ChatUserItemCategoryController(private val i18n: I18n) : Controller<Node> {
    var chatUserCategoryRoot: Label? = null

    override val root: Node?
        get() = chatUserCategoryRoot

    internal fun setChatUserCategory(chatUserCategory: ChatUserCategory?) {
        if (chatUserCategory == null) {
            chatUserCategoryRoot!!.text = null
            return
        }

        chatUserCategoryRoot!!.text = i18n.get(chatUserCategory.i18nKey)
    }
}
