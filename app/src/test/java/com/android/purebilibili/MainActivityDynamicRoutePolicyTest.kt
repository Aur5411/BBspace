package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertEquals

class MainActivityDynamicRoutePolicyTest {

    @Test
    fun dynamicRoute_pointsToDynamicDetailScreen() {
        assertEquals(
            "dynamic_detail/1015637114125025318",
            resolveMainActivityDynamicRoute("1015637114125025318")
        )
    }
}
