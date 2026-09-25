package com.android.purebilibili.core.store.home

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.mapHomeSettingsFromPreferences
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object HomeSettingsStore {

    internal fun mapFromPreferences(preferences: Preferences): HomeSettings {
        return mapHomeSettingsFromPreferences(preferences)
    }

    fun observe(context: Context): Flow<HomeSettings> {
        return context.settingsDataStore.data
            .map(::mapFromPreferences)
            .distinctUntilChanged()
    }
}
