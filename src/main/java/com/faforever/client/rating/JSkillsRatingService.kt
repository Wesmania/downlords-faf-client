package com.faforever.client.rating

import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.TrueSkill
import com.faforever.client.replay.Replay
import com.faforever.client.replay.Replay.PlayerStats
import jskills.GameInfo
import jskills.Rating
import jskills.Team
import jskills.TrueSkillCalculator
import org.springframework.stereotype.Service
import java.util.stream.Collectors

@Service
class JSkillsRatingService(clientProperties: ClientProperties) : RatingService {
    private val gameInfo: GameInfo

    init {
        val trueSkill = clientProperties.getTrueSkill()
        gameInfo = GameInfo(trueSkill.getInitialMean(), trueSkill.getInitialStandardDeviation(), trueSkill.getBeta(),
                trueSkill.getDynamicFactor(), trueSkill.getDrawProbability())
    }

    override fun calculateQuality(replay: Replay): Double {
        val teams = replay.teamPlayerStats.values
        return if (teams.size < 2) {
            java.lang.Double.NaN
        } else TrueSkillCalculator.calculateMatchQuality(gameInfo, teams.stream()
                .map<Team> { players ->
                    val team = Team()
                    players.forEach { stats ->
                        team.addPlayer(
                                jskills.Player<T>(stats.getPlayerId()), Rating(stats.getBeforeMean(), stats.getBeforeDeviation())
                        )
                    }
                    team.toDouble()
                }
                .collect<List<ITeam>, Any>(Collectors.toList<ITeam>()))
    }
}
