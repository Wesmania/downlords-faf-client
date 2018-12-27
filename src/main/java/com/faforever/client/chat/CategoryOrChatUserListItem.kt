package com.faforever.client.chat

import lombok.Value

/**
 * Represents either a chat user or a chat user category.
 *
 *
 * All attempts to use a tree table view failed (bugs everywhere) so it was decided to use a simple list view that
 * contains "category" items.
 *
 *
 * If it's a category-only object, `user` will be `null`. If it's a chat user, `category` will be
 * `null`.
 */
@Value
internal class CategoryOrChatUserListItem {
    var category: ChatUserCategory? = null
    var user: ChatChannelUser? = null
}
