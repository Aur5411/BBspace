package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.Page
import java.util.Locale

enum class PagesSelectorPresentation {
    HorizontalPreview,
    InlineGrid
}

data class PageSelectorGroup(
    val key: String,
    val label: String,
    val count: Int
)

data class PagesSelectorLayoutPolicy(
    val presentation: PagesSelectorPresentation,
    val gridColumns: Int,
    val previewItemWidthDp: Int,
    val previewVisibleItems: Int,
    val horizontalPaddingDp: Int,
    val gridItemMinHeightDp: Int,
    val maxGridHeightDp: Int
)

fun resolvePagesSelectorLayoutPolicy(
    widthDp: Int,
    isLandscape: Boolean,
    pagesCount: Int,
    forceGridMode: Boolean
): PagesSelectorLayoutPolicy {
    val gridColumns = when {
        widthDp >= 1200 -> 5
        widthDp >= 900 -> 4
        widthDp >= 600 -> 3
        isLandscape -> 3
        else -> 2
    }

    val presentation = when {
        forceGridMode -> PagesSelectorPresentation.InlineGrid
        isLandscape -> PagesSelectorPresentation.InlineGrid
        widthDp >= 600 -> PagesSelectorPresentation.InlineGrid
        pagesCount <= 8 -> PagesSelectorPresentation.InlineGrid
        else -> PagesSelectorPresentation.HorizontalPreview
    }

    val previewItemWidthDp = when {
        widthDp >= 900 -> 188
        widthDp >= 600 -> 160
        else -> 136
    }

    val maxGridHeightDp = when {
        widthDp >= 1200 -> 520
        widthDp >= 840 -> 500
        isLandscape -> 420
        else -> 460
    }

    return PagesSelectorLayoutPolicy(
        presentation = presentation,
        gridColumns = gridColumns,
        previewItemWidthDp = previewItemWidthDp,
        previewVisibleItems = 6,
        horizontalPaddingDp = 16,
        gridItemMinHeightDp = 60,
        maxGridHeightDp = maxGridHeightDp
    )
}

fun shouldShowPagesExpandAction(
    policy: PagesSelectorLayoutPolicy,
    pagesCount: Int
): Boolean {
    return policy.presentation == PagesSelectorPresentation.HorizontalPreview &&
        pagesCount > policy.previewVisibleItems
}

fun resolvePagesSelectorBottomContentPaddingDp(
    navigationBarBottomDp: Int,
    baseBottomPaddingDp: Int = 24
): Int {
    return baseBottomPaddingDp + navigationBarBottomDp.coerceAtLeast(0)
}

private val numericPrefixRegex = Regex("^\\s*(\\d+(?:\\.\\d+)*)")
private val cnChapterRegex = Regex("第\\s*(\\d+)\\s*[章节讲]")
private const val OTHER_GROUP_KEY = "other"

fun resolvePageSelectorGroups(pages: List<Page>): List<PageSelectorGroup> {
    if (pages.isEmpty()) return emptyList()

    val countByGroup = linkedMapOf<String, Int>()
    pages.forEach { page ->
        val key = resolvePageChapterGroupKey(page)
        countByGroup[key] = (countByGroup[key] ?: 0) + 1
    }

    return countByGroup.entries
        .sortedWith(
            compareBy<Map.Entry<String, Int>>(
                { entry ->
                    entry.key.toIntOrNull() ?: Int.MAX_VALUE
                },
                { entry ->
                    entry.key
                }
            )
        )
        .map { entry ->
            PageSelectorGroup(
                key = entry.key,
                label = if (entry.key == OTHER_GROUP_KEY) "其他" else "${entry.key}章",
                count = entry.value
            )
        }
}

fun filterPageIndicesForSelector(
    pages: List<Page>,
    selectedGroupKey: String?,
    query: String
): List<Int> {
    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    return pages.indices.filter { index ->
        val page = pages[index]
        val groupPassed = selectedGroupKey.isNullOrBlank() ||
            resolvePageChapterGroupKey(page) == selectedGroupKey
        val searchPassed = if (normalizedQuery.isBlank()) {
            true
        } else {
            val partText = page.part.lowercase(Locale.getDefault())
            val pText = "p${page.page}".lowercase(Locale.getDefault())
            partText.contains(normalizedQuery) || pText.contains(normalizedQuery)
        }
        groupPassed && searchPassed
    }
}

fun resolvePageChapterGroupKey(page: Page): String {
    val part = page.part.trim()
    val numericPrefix = numericPrefixRegex.find(part)?.groupValues?.getOrNull(1)
    if (!numericPrefix.isNullOrBlank()) {
        return numericPrefix.substringBefore(".")
    }
    val cnPrefix = cnChapterRegex.find(part)?.groupValues?.getOrNull(1)
    if (!cnPrefix.isNullOrBlank()) {
        return cnPrefix
    }
    return OTHER_GROUP_KEY
}
