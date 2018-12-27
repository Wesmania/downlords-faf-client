package com.faforever.client.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.CacheResolver
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.springframework.cache.interceptor.SimpleKeyGenerator
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import java.util.Arrays

import com.faforever.client.config.CacheNames.ACHIEVEMENTS
import com.faforever.client.config.CacheNames.ACHIEVEMENT_IMAGES
import com.faforever.client.config.CacheNames.AVAILABLE_AVATARS
import com.faforever.client.config.CacheNames.AVATARS
import com.faforever.client.config.CacheNames.CLAN
import com.faforever.client.config.CacheNames.COOP_LEADERBOARD
import com.faforever.client.config.CacheNames.COOP_MAPS
import com.faforever.client.config.CacheNames.COUNTRY_FLAGS
import com.faforever.client.config.CacheNames.FEATURED_MODS
import com.faforever.client.config.CacheNames.FEATURED_MOD_FILES
import com.faforever.client.config.CacheNames.GLOBAL_LEADERBOARD
import com.faforever.client.config.CacheNames.LADDER_1V1_LEADERBOARD
import com.faforever.client.config.CacheNames.MAPS
import com.faforever.client.config.CacheNames.MAP_PREVIEW
import com.faforever.client.config.CacheNames.MODS
import com.faforever.client.config.CacheNames.MOD_THUMBNAIL
import com.faforever.client.config.CacheNames.NEWS
import com.faforever.client.config.CacheNames.RATING_HISTORY
import com.faforever.client.config.CacheNames.STATISTICS
import com.faforever.client.config.CacheNames.THEME_IMAGES
import com.faforever.client.config.CacheNames.URL_PREVIEW
import com.github.benmanes.caffeine.cache.Caffeine.newBuilder
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

@Configuration
@EnableCaching
class CacheConfig : CachingConfigurerSupport() {

    @Bean
    override fun cacheManager(): CacheManager? {
        val simpleCacheManager = SimpleCacheManager()
        simpleCacheManager.setCaches(Arrays.asList(
                CaffeineCache(STATISTICS, newBuilder().maximumSize(10).expireAfterWrite(20, MINUTES).build()),
                CaffeineCache(ACHIEVEMENTS, newBuilder().expireAfterWrite(10, MINUTES).build()),
                CaffeineCache(MODS, newBuilder().expireAfterWrite(10, MINUTES).build()),
                CaffeineCache(MAPS, newBuilder().expireAfterWrite(10, MINUTES).build()),
                CaffeineCache(GLOBAL_LEADERBOARD, newBuilder().maximumSize(1).expireAfterAccess(5, MINUTES).build()),
                CaffeineCache(LADDER_1V1_LEADERBOARD, newBuilder().maximumSize(1).expireAfterAccess(5, MINUTES).build()),
                CaffeineCache(AVAILABLE_AVATARS, newBuilder().expireAfterAccess(30, SECONDS).build()),
                CaffeineCache(COOP_MAPS, newBuilder().expireAfterAccess(10, SECONDS).build()),
                CaffeineCache(NEWS, newBuilder().expireAfterWrite(1, MINUTES).build()),
                CaffeineCache(RATING_HISTORY, newBuilder().expireAfterWrite(1, MINUTES).build()),
                CaffeineCache(COOP_LEADERBOARD, newBuilder().expireAfterWrite(1, MINUTES).build()),
                CaffeineCache(CLAN, newBuilder().expireAfterWrite(1, MINUTES).build()),
                CaffeineCache(FEATURED_MODS, newBuilder().build()),
                CaffeineCache(FEATURED_MOD_FILES, newBuilder().expireAfterWrite(10, MINUTES).build()),

                // Images should only be cached as long as they are in use. This avoids loading an image multiple times, while
                // at the same time it doesn't prevent unused images from being garbage collected.
                CaffeineCache(ACHIEVEMENT_IMAGES, newBuilder().weakValues().build()),
                CaffeineCache(AVATARS, newBuilder().weakValues().build()),
                CaffeineCache(URL_PREVIEW, newBuilder().weakValues().expireAfterAccess(30, MINUTES).build()),
                CaffeineCache(MAP_PREVIEW, newBuilder().weakValues().build()),
                CaffeineCache(COUNTRY_FLAGS, newBuilder().weakValues().build()),
                CaffeineCache(THEME_IMAGES, newBuilder().weakValues().build()),
                CaffeineCache(MOD_THUMBNAIL, newBuilder().weakValues().build()
                )))
        return simpleCacheManager
    }

    override fun cacheResolver(): CacheResolver? {
        return null
    }

    @Bean
    override fun keyGenerator(): KeyGenerator? {
        return SimpleKeyGenerator()
    }

    override fun errorHandler(): CacheErrorHandler? {
        return SimpleCacheErrorHandler()
    }
}
