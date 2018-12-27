package com.faforever.client.preferences

interface PreferenceUpdateListener {

    /**
     * Called whenever the preference file has been updated.
     */
    fun onPreferencesUpdated(preferences: Preferences)
}
