package com.android.purebilibili.core.coroutines

import org.junit.Assert.assertEquals
import org.junit.Test

class AppScopeUsagePolicyTest {

    @Test
    fun homePreloadUsesApplicationIoScope() {
        assertEquals(
            AsyncLaunchPolicy(scopeOwner = ScopeOwner.APP, thread = LaunchThread.IO),
            resolveHomePreloadLaunchPolicy()
        )
    }

    @Test
    fun localProxyWarmupUsesApplicationIoScope() {
        assertEquals(
            AsyncLaunchPolicy(scopeOwner = ScopeOwner.APP, thread = LaunchThread.IO),
            resolveLocalProxyWarmupLaunchPolicy()
        )
    }

    @Test
    fun shortLinkResolutionUsesLifecycleAwareMainScope() {
        assertEquals(
            AsyncLaunchPolicy(scopeOwner = ScopeOwner.LIFECYCLE, thread = LaunchThread.MAIN),
            resolveShortLinkLaunchPolicy()
        )
    }
}
