package com.android.purebilibili.feature.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileHeaderClickPolicyTest {

    @Test
    fun guestHeaderClickPolicy_enablesLoginEntryForLoggedOutUser() {
        assertTrue(
            shouldEnableProfileHeaderLoginClick(
                isLogin = false
            )
        )
    }

    @Test
    fun guestHeaderClickPolicy_disablesHeaderLoginEntryForLoggedInUser() {
        assertFalse(
            shouldEnableProfileHeaderLoginClick(
                isLogin = true
            )
        )
    }
}
