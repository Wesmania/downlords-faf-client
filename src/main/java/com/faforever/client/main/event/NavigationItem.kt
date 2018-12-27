package com.faforever.client.main.event

import java.util.HashMap

enum class NavigationItem private constructor(val fxmlFile: String) {
    NEWS("theme/news.fxml"),
    CHAT("theme/chat/chat.fxml"),
    PLAY("theme/play/play.fxml"),
    VAULT("theme/vault/vault.fxml"),
    LEADERBOARD("theme/leaderboard/leaderboards.fxml"),
    TOURNAMENTS("theme/tournaments/tournaments.fxml"),
    UNITS("theme/units.fxml");


    companion object {

        private val fromString: HashMap<String, NavigationItem>

        init {
            fromString = HashMap()
            for (item in values()) {
                fromString[item.name] = item
            }
        }

        fun fromString(string: String?): NavigationItem {
            return if (string == null) {
                NEWS
            } else fromString[string]
        }
    }
}
