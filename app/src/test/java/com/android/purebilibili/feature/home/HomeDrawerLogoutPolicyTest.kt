package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeDrawerLogoutPolicyTest {

    @Test
    fun `uses explicit logout callback when provided`() {
        var logoutCount = 0
        var profileCount = 0

        val action = resolveHomeDrawerLogoutAction(
            onLogout = { logoutCount += 1 },
            onProfileClick = { profileCount += 1 }
        )

        action()

        assertEquals(1, logoutCount)
        assertEquals(0, profileCount)
    }

    @Test
    fun `falls back to profile callback when logout callback missing`() {
        var profileCount = 0

        val action = resolveHomeDrawerLogoutAction(
            onLogout = null,
            onProfileClick = { profileCount += 1 }
        )

        action()

        assertEquals(1, profileCount)
    }
}
