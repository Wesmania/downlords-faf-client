package com.faforever.client.mod

import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class ReviewsSummary {
    private val id: StringProperty
    private val positive: FloatProperty
    private val negative: FloatProperty
    private val score: FloatProperty
    private val reviews: IntegerProperty
    private val lowerBound: FloatProperty

    init {
        id = SimpleStringProperty()
        positive = SimpleFloatProperty()
        negative = SimpleFloatProperty()
        score = SimpleFloatProperty()
        reviews = SimpleIntegerProperty()
        lowerBound = SimpleFloatProperty()
    }

    fun getId(): String {
        return id.get()
    }

    fun setId(id: String) {
        this.id.set(id)
    }

    fun idProperty(): StringProperty {
        return id
    }

    fun getPositive(): Float {
        return positive.get()
    }

    fun setPositive(positive: Float) {
        this.positive.set(positive)
    }

    fun positiveProperty(): FloatProperty {
        return positive
    }

    fun getNegative(): Float {
        return negative.get()
    }

    fun setNegative(negative: Float) {
        this.negative.set(negative)
    }

    fun negativeProperty(): FloatProperty {
        return negative
    }

    fun getScore(): Float {
        return score.get()
    }

    fun setScore(score: Float) {
        this.score.set(score)
    }

    fun scoreProperty(): FloatProperty {
        return score
    }

    fun getReviews(): Int {
        return reviews.get()
    }

    fun setReviews(reviews: Int) {
        this.reviews.set(reviews)
    }

    fun reviewsProperty(): IntegerProperty {
        return reviews
    }

    fun getLowerBound(): Float {
        return lowerBound.get()
    }

    fun setLowerBound(lowerBound: Float) {
        this.lowerBound.set(lowerBound)
    }

    fun lowerBoundProperty(): FloatProperty {
        return lowerBound
    }

    companion object {

        fun fromDto(dto: com.faforever.client.api.dto.ReviewsSummary?): ReviewsSummary {
            val reviewsSummary = ReviewsSummary()
            if (dto != null) {
                reviewsSummary.setId(dto.getId())
                reviewsSummary.setPositive(dto.getPositive())
                reviewsSummary.setNegative(dto.getNegative())
                reviewsSummary.setScore(dto.getScore())
                reviewsSummary.setLowerBound(dto.getLowerBound())
            }
            return reviewsSummary
        }
    }
}
