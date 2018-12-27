package com.faforever.client.map

import com.faforever.client.main.event.NavigateEvent
import com.faforever.client.main.event.NavigationItem
import com.faforever.client.vault.search.SearchController.SearchConfig

class SearchMapVaultEvent : NavigateEvent(NavigationItem.VAULT) {
    internal var searchConfig: SearchConfig? = null
}
