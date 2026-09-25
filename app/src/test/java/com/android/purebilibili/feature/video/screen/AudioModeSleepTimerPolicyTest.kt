package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioModeSleepTimerPolicyTest {

    @Test
    fun `parseAudioModeSleepTimerInput accepts plain minute values`() {
        assertEquals(45, parseAudioModeSleepTimerInput("45"))
        assertEquals(90, parseAudioModeSleepTimerInput(" 90 "))
    }

    @Test
    fun `parseAudioModeSleepTimerInput accepts hour minute formats`() {
        assertEquals(90, parseAudioModeSleepTimerInput("1:30"))
        assertEquals(125, parseAudioModeSleepTimerInput("2：05"))
    }

    @Test
    fun `parseAudioModeSleepTimerInput rejects invalid values`() {
        assertNull(parseAudioModeSleepTimerInput(""))
        assertNull(parseAudioModeSleepTimerInput("0"))
        assertNull(parseAudioModeSleepTimerInput("1:90"))
        assertNull(parseAudioModeSleepTimerInput("abc"))
    }

    @Test
    fun `formatAudioModeSleepTimerButtonLabel formats common states`() {
        assertEquals("定时关闭", formatAudioModeSleepTimerButtonLabel(null))
        assertEquals("30分钟", formatAudioModeSleepTimerButtonLabel(30))
        assertEquals("1小时", formatAudioModeSleepTimerButtonLabel(60))
        assertEquals("1:30", formatAudioModeSleepTimerButtonLabel(90))
    }

    @Test
    fun `formatAudioModeSleepTimerInput prefers compact editable values`() {
        assertEquals("", formatAudioModeSleepTimerInput(null))
        assertEquals("45", formatAudioModeSleepTimerInput(45))
        assertEquals("60", formatAudioModeSleepTimerInput(60))
        assertEquals("1:30", formatAudioModeSleepTimerInput(90))
    }
}
