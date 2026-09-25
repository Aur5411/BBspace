package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitProgressSyncPolicyTest {

    @Test
    fun applySyncWhenSnapshotBvidMatchesCurrentBvid() {
        assertTrue(
            shouldApplyPortraitProgressSync(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 100L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun doNotApplySyncWhenSnapshotBvidMismatchesCurrentBvid() {
        assertFalse(
            shouldApplyPortraitProgressSync(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 100L,
                currentBvid = "BV9xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun doNotApplySyncWhenSnapshotBvidBlank() {
        assertFalse(
            shouldApplyPortraitProgressSync(
                snapshotBvid = " ",
                snapshotCid = 100L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun doNotApplySyncWhenSnapshotCidMismatchesCurrentCid() {
        assertFalse(
            shouldApplyPortraitProgressSync(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 101L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 202L
            )
        )
    }

    @Test
    fun applySyncWhenCurrentCidUnknownButBvidMatches() {
        assertTrue(
            shouldApplyPortraitProgressSync(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 101L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 0L
            )
        )
    }
}
