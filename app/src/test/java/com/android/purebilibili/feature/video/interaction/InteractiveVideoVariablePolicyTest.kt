package com.android.purebilibili.feature.video.interaction

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InteractiveVideoVariablePolicyTest {

    @Test
    fun `empty condition should always pass`() {
        assertTrue(evaluateInteractiveChoiceCondition("", emptyMap()))
        assertTrue(evaluateInteractiveChoiceCondition("   ", emptyMap()))
    }

    @Test
    fun `condition should evaluate numeric comparisons with and or`() {
        val variables = mapOf("score" to 75.0, "flag" to 1.0)
        assertTrue(
            evaluateInteractiveChoiceCondition(
                "\$score>=70 && \$score<=80",
                variables
            )
        )
        assertTrue(
            evaluateInteractiveChoiceCondition(
                "\$score>=90 || \$flag==1",
                variables
            )
        )
        assertFalse(
            evaluateInteractiveChoiceCondition(
                "\$score<60 && \$flag==1",
                variables
            )
        )
    }

    @Test
    fun `condition should support arithmetic precedence and parentheses`() {
        val variables = mapOf("a" to 3.0, "b" to 4.0, "threshold" to 4.0)
        assertTrue(
            evaluateInteractiveChoiceCondition(
                "\$a+\$b*2>=11 && (\$a+\$b)>=7",
                variables
            )
        )
        assertFalse(
            evaluateInteractiveChoiceCondition(
                "(\$a+\$b)/2>\$threshold",
                variables
            )
        )
    }

    @Test
    fun `missing variable should fallback to zero`() {
        assertTrue(evaluateInteractiveChoiceCondition("\$unknown<=0", emptyMap()))
        assertFalse(evaluateInteractiveChoiceCondition("\$unknown>0", emptyMap()))
    }

    @Test
    fun `invalid condition syntax should degrade to true`() {
        assertTrue(evaluateInteractiveChoiceCondition("this is not expression", emptyMap()))
    }

    @Test
    fun `native action should apply assignment arithmetic and copy`() {
        val variables = mutableMapOf<String, Double>()
        applyInteractiveNativeAction("\$a=1;\$b=\$a+2;\$c=\$b", variables)
        assertEquals(1.0, variables["a"])
        assertEquals(3.0, variables["b"])
        assertEquals(3.0, variables["c"])
    }

    @Test
    fun `native action should support multiply divide and parentheses`() {
        val variables = mutableMapOf("a" to 3.0, "b" to 6.0)
        applyInteractiveNativeAction("\$x=(\$a+\$b)*2;\$y=\$x/3", variables)
        assertEquals(18.0, variables["x"])
        assertEquals(6.0, variables["y"])
    }

    @Test
    fun `native action should support unary minus`() {
        val variables = mutableMapOf<String, Double>()
        applyInteractiveNativeAction("\$n=-3;\$m=\$n*2", variables)
        assertEquals(-3.0, variables["n"])
        assertEquals(-6.0, variables["m"])
    }

    @Test
    fun `native action should ignore invalid statements`() {
        val variables = mutableMapOf("x" to 2.0)
        applyInteractiveNativeAction("random();\${'$'}x=\${'$'}x*", variables)
        assertEquals(2.0, variables["x"])
    }
}
