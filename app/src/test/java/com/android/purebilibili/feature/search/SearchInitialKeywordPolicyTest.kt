package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchInitialKeywordPolicyTest {

    @Test
    fun appliesInitialKeyword_whenKeywordIsNonBlank_andSearchHasNotRun() {
        assertTrue(
            shouldApplyInitialSearchKeyword(
                initialKeyword = "黑神话",
                currentQuery = "",
                showResults = false
            )
        )
    }

    @Test
    fun skipsInitialKeyword_whenSameKeywordResultsAreAlreadyShowing() {
        assertFalse(
            shouldApplyInitialSearchKeyword(
                initialKeyword = "黑神话",
                currentQuery = "黑神话",
                showResults = true
            )
        )
    }

    @Test
    fun skipsInitialKeyword_whenKeywordIsBlank() {
        assertFalse(
            shouldApplyInitialSearchKeyword(
                initialKeyword = "   ",
                currentQuery = "",
                showResults = false
            )
        )
    }
}
