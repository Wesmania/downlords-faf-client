package com.faforever.client.leaderboard

import com.faforever.client.api.dto.GlobalLeaderboardEntry
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry
import javafx.beans.property.DoubleProperty
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class LeaderboardEntry {

    private val username: StringProperty
    private val rank: IntegerProperty
    private val rating: DoubleProperty
    private val gamesPlayed: IntegerProperty
    private val winLossRatio: FloatProperty

    init {
        username = SimpleStringProperty()
        rank = SimpleIntegerProperty()
        rating = SimpleDoubleProperty()
        gamesPlayed = SimpleIntegerProperty()
        winLossRatio = SimpleFloatProperty()
    }

    fun getUsername(): String {
        return username.get()
    }

    fun setUsername(username: String) {
        this.username.set(username)
    }

    fun usernameProperty(): StringProperty {
        return username
    }

    fun getRank(): Int {
        return rank.get()
    }

    fun setRank(rank: Int) {
        this.rank.set(rank)
    }

    fun rankProperty(): IntegerProperty {
        return rank
    }

    fun getRating(): Double {
        return rating.get()
    }

    fun setRating(rating: Double) {
        this.rating.set(rating)
    }

    fun ratingProperty(): DoubleProperty {
        return rating
    }

    fun getGamesPlayed(): Int {
        return gamesPlayed.get()
    }

    fun setGamesPlayed(gamesPlayed: Int) {
        this.gamesPlayed.set(gamesPlayed)
    }

    fun gamesPlayedProperty(): IntegerProperty {
        return gamesPlayed
    }

    fun getWinLossRatio(): Float {
        return winLossRatio.get()
    }

    fun setWinLossRatio(winLossRatio: Float) {
        this.winLossRatio.set(winLossRatio)
    }

    fun winLossRatioProperty(): FloatProperty {
        return winLossRatio
    }

    override fun hashCode(): Int {
        return if (username.get() != null) username.get().hashCode() else 0
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as LeaderboardEntry?

        return !if (username.get() != null) !username.get().equals(that!!.username.get(), ignoreCase = true) else that!!.username.get() != null

    }

    override fun toString(): String {
        return "Ranked1v1EntryBean{" +
                "username=" + username.get() +
                '}'.toString()
    }

    companion object {

        fun fromLadder1v1(entry: Ladder1v1LeaderboardEntry): LeaderboardEntry {
            val leaderboardEntry = LeaderboardEntry()
            leaderboardEntry.setUsername(entry.getName())
            leaderboardEntry.setGamesPlayed(entry.getNumGames())
            leaderboardEntry.setRank(entry.getRank())
            leaderboardEntry.setRating(entry.getRating())
            leaderboardEntry.setWinLossRatio(entry.getWonGames() / entry.getNumGames() as Float)
            return leaderboardEntry
        }

        fun fromGlobalRating(globalLeaderboardEntry: GlobalLeaderboardEntry): LeaderboardEntry {
            val leaderboardEntry = LeaderboardEntry()
            leaderboardEntry.setUsername(globalLeaderboardEntry.getName())
            leaderboardEntry.setGamesPlayed(globalLeaderboardEntry.getNumGames())
            leaderboardEntry.setRank(globalLeaderboardEntry.getRank())
            leaderboardEntry.setRating(globalLeaderboardEntry.getRating())
            return leaderboardEntry
        }
    }
}
