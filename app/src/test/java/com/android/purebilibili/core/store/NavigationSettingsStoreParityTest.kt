package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.purebilibili.core.store.navigation.NavigationSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationSettingsStoreParityTest {

    @Test
    fun `navigation store maps defaults the same way as settings manager policy`() {
        val prefs = mutablePreferencesOf()

        assertEquals(
            mapAppNavigationSettingsFromPreferences(prefs),
            NavigationSettingsStore.mapFromPreferences(prefs)
        )
    }

    @Test
    fun `navigation store maps populated preferences the same way as settings manager policy`() {
        val prefs = mutablePreferencesOf(
            intPreferencesKey("bottom_bar_visibility_mode") to SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE.value,
            stringPreferencesKey("bottom_bar_order") to "PROFILE,HOME,DYNAMIC,HISTORY",
            stringPreferencesKey("bottom_bar_visible_tabs") to "HOME,PROFILE,HISTORY",
            booleanPreferencesKey("tablet_use_sidebar") to true
        )

        assertEquals(
            mapAppNavigationSettingsFromPreferences(prefs),
            NavigationSettingsStore.mapFromPreferences(prefs)
        )
    }
}
