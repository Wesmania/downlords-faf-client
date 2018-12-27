package com.faforever.client.config

class CacheNames private constructor() {

    init {
        throw AssertionError("Not instantiatable")
    }

    companion object {

       const val AVATARS = "avatars"
       const val COUNTRY_FLAGS = "countryFlags"
       const val MAP_PREVIEW = "mapPreview"
       const val URL_PREVIEW = "urlPreview"
       const val STATISTICS = "statistics"
       const val ACHIEVEMENT_IMAGES = "achievementImages"
       const val ACHIEVEMENTS = "achievements"
       const val MODS = "mods"
       const val LADDER_1V1_LEADERBOARD = "ladder1v1Leaderboard"
       const val GLOBAL_LEADERBOARD = "globalLeaderboard"
       const val MAPS = "maps"
       const val THEME_IMAGES = "themeImages"
       const val MOD_THUMBNAIL = "modThumbnail"
       const val COOP_MAPS = "coopMaps"
       const val AVAILABLE_AVATARS = "availableAvatars"
       const val NEWS = "news"
       const val RATING_HISTORY = "ratingHistory"
       const val FEATURED_MODS = "featuredMods"
       const val FEATURED_MOD_FILES = "featuredModFiles"
       const val COOP_LEADERBOARD = "coopLeaderboard"
       const val CLAN = "clan"
    }
}
