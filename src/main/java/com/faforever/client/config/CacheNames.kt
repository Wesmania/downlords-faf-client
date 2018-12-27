package com.faforever.client.config

class CacheNames private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

        val AVATARS = "avatars"
        val COUNTRY_FLAGS = "countryFlags"
        val MAP_PREVIEW = "mapPreview"
        val URL_PREVIEW = "urlPreview"
        val STATISTICS = "statistics"
        val ACHIEVEMENT_IMAGES = "achievementImages"
        val ACHIEVEMENTS = "achievements"
        val MODS = "mods"
        val LADDER_1V1_LEADERBOARD = "ladder1v1Leaderboard"
        val GLOBAL_LEADERBOARD = "globalLeaderboard"
        val MAPS = "maps"
        val THEME_IMAGES = "themeImages"
        val MOD_THUMBNAIL = "modThumbnail"
        val COOP_MAPS = "coopMaps"
        val AVAILABLE_AVATARS = "availableAvatars"
        val NEWS = "news"
        val RATING_HISTORY = "ratingHistory"
        val FEATURED_MODS = "featuredMods"
        val FEATURED_MOD_FILES = "featuredModFiles"
        val COOP_LEADERBOARD = "coopLeaderboard"
        val CLAN = "clan"
    }
}
