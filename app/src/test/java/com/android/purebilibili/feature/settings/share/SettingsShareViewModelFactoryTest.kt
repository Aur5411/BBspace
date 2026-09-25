package com.android.purebilibili.feature.settings.share

import android.app.Application
import kotlin.test.Test
import kotlin.test.assertNotNull

class SettingsShareViewModelFactoryTest {

    @Test
    fun androidViewModelFactory_canFindApplicationConstructor() {
        val constructor = SettingsShareViewModel::class.java.getConstructor(Application::class.java)

        assertNotNull(constructor)
    }
}
