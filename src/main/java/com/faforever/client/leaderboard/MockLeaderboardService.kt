package com.faforever.client.leaderboard

import com.faforever.client.FafClientApplication
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.i18n.I18n
import com.faforever.client.task.CompletableTask
import com.faforever.client.task.TaskService
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.CompletableFuture

import com.faforever.client.task.CompletableTask.Priority.HIGH


@Lazy
@Service
@Profile(FafClientApplication.PROFILE_OFFLINE)
class MockLeaderboardService @Inject
constructor(private val taskService: TaskService, private val i18n: I18n) : LeaderboardService {

    override val ladder1v1Stats: CompletableFuture<List<RatingStat>>
        get() = CompletableFuture.completedFuture(emptyList())

    override fun getEntryForPlayer(playerId: Int): CompletableFuture<LeaderboardEntry> {
        return CompletableFuture.completedFuture(createLadderInfoBean("Player #$playerId", 111, 222, 333, 55.55f))
    }

    override fun getEntries(ratingType: KnownFeaturedMod): CompletableFuture<List<LeaderboardEntry>> {
        return taskService.submitTask<>(object : CompletableTask<List<LeaderboardEntry>>(HIGH) {
            @Throws(Exception::class)
            override fun call(): List<LeaderboardEntry> {
                updateTitle("Reading ladder")

                val list = ArrayList<LeaderboardEntry>()
                for (i in 1..10000) {
                    val name = RandomStringUtils.random(10)
                    val rating = (Math.random() * 2500).toInt()
                    val gamecount = (Math.random() * 10000).toInt()
                    val winloss = (Math.random() * 100).toFloat()

                    list.add(createLadderInfoBean(name, i, rating, gamecount, winloss))

                }
                return list
            }
        }).future
    }

    private fun createLadderInfoBean(name: String, rank: Int, rating: Int, gamesPlayed: Int, winLossRatio: Float): LeaderboardEntry {
        val leaderboardEntry = LeaderboardEntry()
        leaderboardEntry.username = name
        leaderboardEntry.rank = rank
        leaderboardEntry.rating = rating.toDouble()
        leaderboardEntry.gamesPlayed = gamesPlayed
        leaderboardEntry.winLossRatio = winLossRatio

        return leaderboardEntry
    }
}
