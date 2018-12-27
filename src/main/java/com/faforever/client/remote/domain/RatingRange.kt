package com.faforever.client.remote.domain

class RatingRange(val min: Int?, val max: Int?) : Comparable<RatingRange> {

    override fun compareTo(o: RatingRange): Int {
        return Integer.compare(min!!, o.min!!)
    }
}
