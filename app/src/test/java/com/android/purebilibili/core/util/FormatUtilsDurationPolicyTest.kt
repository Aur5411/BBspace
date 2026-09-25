package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsDurationPolicyTest {

    @Test
    fun formatDuration_seconds_usesAdaptiveLayout() {
        assertEquals("01:53", FormatUtils.formatDuration(113))
        assertEquals("09:42:32", FormatUtils.formatDuration(34_952))
    }

    @Test
    fun formatDuration_milliseconds_usesAdaptiveLayout() {
        assertEquals("00:04", FormatUtils.formatDuration(4_000L))
        assertEquals("10:05", FormatUtils.formatDuration(605_000L))
        assertEquals("01:02:03", FormatUtils.formatDuration(3_723_000L))
    }
}
