package com.faforever.client.preferences

import com.faforever.client.vault.search.SearchController.SortConfig
import com.faforever.client.vault.search.SearchController.SortOrder
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty

class VaultPrefs {
    private val onlineReplaySortConfig: ObjectProperty<SortConfig>
    private val mapSortConfig: ObjectProperty<SortConfig>
    private val modVaultConfig: ObjectProperty<SortConfig>

    init {
        onlineReplaySortConfig = SimpleObjectProperty(SortConfig("startTime", SortOrder.DESC))
        mapSortConfig = SimpleObjectProperty(SortConfig("statistics.plays", SortOrder.DESC))
        modVaultConfig = SimpleObjectProperty(SortConfig("latestVersion.createTime", SortOrder.DESC))
    }

    fun getOnlineReplaySortConfig(): SortConfig {
        return onlineReplaySortConfig.get()
    }

    fun setOnlineReplaySortConfig(onlineReplaySortConfig: SortConfig) {
        this.onlineReplaySortConfig.set(onlineReplaySortConfig)
    }

    fun onlineReplaySortConfigProperty(): ObjectProperty<SortConfig> {
        return onlineReplaySortConfig
    }

    fun getMapSortConfig(): SortConfig {
        return mapSortConfig.get()
    }

    fun setMapSortConfig(mapSortConfig: SortConfig) {
        this.mapSortConfig.set(mapSortConfig)
    }

    fun mapSortConfigProperty(): ObjectProperty<SortConfig> {
        return mapSortConfig
    }

    fun getModVaultConfig(): SortConfig {
        return modVaultConfig.get()
    }

    fun setModVaultConfig(modVaultConfig: SortConfig) {
        this.modVaultConfig.set(modVaultConfig)
    }

    fun modVaultConfigProperty(): ObjectProperty<SortConfig> {
        return modVaultConfig
    }
}
