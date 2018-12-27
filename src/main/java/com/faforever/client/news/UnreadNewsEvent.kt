package com.faforever.client.news

class UnreadNewsEvent(private val hasUnreadNews: Boolean) {

    fun hasUnreadNews(): Boolean {
        return hasUnreadNews
    }
}
