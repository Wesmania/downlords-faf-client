package com.faforever.client.map

import lombok.Data

import java.util.HashMap

@Data
class MapSize
/**
 * @param widthInPixels in kilometers
 * @param heightInPixels in kilometers
 */
private constructor(
        /**
         * The map width in pixels. One kilometer equals 51.2 pixels.
         */
        private val widthInPixels: Int,
        /**
         * The map height in pixels. One kilometer equals 51.2 pixels.
         */
        private val heightInPixels: Int) : Comparable<MapSize> {

    val widthInKm: Int
        get() = (widthInPixels / MAP_SIZE_FACTOR).toInt()

    val heightInKm: Int
        get() = (heightInPixels / MAP_SIZE_FACTOR).toInt()

    override fun compareTo(o: MapSize): Int {
        val dimension = widthInPixels * heightInPixels
        val otherDimension = o.widthInPixels * o.heightInPixels

        return if (dimension == otherDimension) {

            Integer.compare(widthInPixels, o.widthInPixels)
        } else Integer.compare(dimension, otherDimension)

    }

    override fun hashCode(): Int {
        var result = widthInPixels
        result = 31 * result + heightInPixels
        return result
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val mapSize = o as MapSize?

        return widthInPixels == mapSize!!.widthInPixels && heightInPixels == mapSize.heightInPixels
    }

    override fun toString(): String {
        return String.format("%dx%d", widthInPixels, heightInPixels)
    }

    companion object {

        private val MAP_SIZE_FACTOR = 51.2f

        private val cache = HashMap<String, MapSize>()

        fun valueOf(widthInPixels: Int, heightInPixels: Int): MapSize {
            val cacheKey = widthInPixels.toString() + heightInPixels.toString()
            if (cache.containsKey(cacheKey)) {
                return cache[cacheKey]
            }

            val mapSize = MapSize(widthInPixels, heightInPixels)
            cache[cacheKey] = mapSize
            return mapSize
        }
    }
}
