package com.faforever.client.news


import com.faforever.client.theme.UiService

import java.util.HashMap
import java.util.Locale

enum class NewsCategory private constructor(private val name: String, val imagePath: String) {

    SERVER_UPDATE("server update", UiService.SERVER_UPDATE_NEWS_IMAGE),
    TOURNAMENT("tournament", UiService.TOURNAMENT_NEWS_IMAGE),
    FA_UPDATE("fa update", UiService.FA_UPDATE_NEWS_IMAGE),
    LOBBY_UPDATE("lobby update", UiService.LOBBY_UPDATE_NEWS_IMAGE),
    BALANCE("balance", UiService.BALANCE_NEWS_IMAGE),
    WEBSITE("website", UiService.WEBSITE_NEWS_IMAGE),
    CAST("cast", UiService.CAST_NEWS_IMAGE),
    PODCAST("podcast", UiService.PODCAST_NEWS_IMAGE),
    FEATURED_MOD("featured mods", UiService.FEATURED_MOD_NEWS_IMAGE),
    DEVELOPMENT("development update", UiService.DEVELOPMENT_NEWS_IMAGE),
    UNCATEGORIZED("uncategorized", UiService.DEFAULT_NEWS_IMAGE),
    LADDER("ladder", UiService.LADDER_NEWS_IMAGE);


    companion object {

        private val fromString: MutableMap<String, NewsCategory>

        init {
            fromString = HashMap()
            for (newsCategory in values()) {
                fromString[newsCategory.name] = newsCategory
            }
        }

        fun fromString(string: String?): NewsCategory? {
            if (string == null) {
                return null
            }
            val toLower = string.toLowerCase(Locale.US)
            return if (!fromString.containsKey(toLower)) {
                NewsCategory.UNCATEGORIZED
            } else fromString[toLower]
        }
    }

}
