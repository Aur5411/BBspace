package com.android.purebilibili.core.ui.blur

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class UnifiedBlurShapePolicyTest {

    @Test
    fun defaultsToRectangleEdgeTreatmentWhenNoShapeIsProvided() {
        val result = resolveUnifiedBlurredEdgeTreatment(shape = null)

        assertEquals(BlurredEdgeTreatment.Rectangle, result)
    }

    @Test
    fun preservesRoundedShapeForBlurredEdgeTreatment() {
        val shape = RoundedCornerShape(50.dp)

        val result = resolveUnifiedBlurredEdgeTreatment(shape = shape)

        assertEquals(shape, result.shape)
    }
}
