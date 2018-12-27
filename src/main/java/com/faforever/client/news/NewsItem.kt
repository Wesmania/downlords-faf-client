package com.faforever.client.news

import lombok.Data

import java.util.Date

@Data
internal class NewsItem {

    private val author: String? = null
    private val link: String? = null
    private val title: String? = null
    private val content: String? = null
    private val date: Date? = null
    private val newsCategory: NewsCategory? = null
}
