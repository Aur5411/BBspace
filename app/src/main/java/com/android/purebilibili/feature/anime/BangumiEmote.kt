package com.android.purebilibili.feature.anime

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.android.purebilibili.R

private val EMOTE_REGEX = Regex("\\(bgm(\\d{1,3})\\)")

private val EMOTE_SIZE_SP = 20f

private val STICKER_RES: IntArray = intArrayOf(
        R.drawable.bgm_01,
        R.drawable.bgm_02,
        R.drawable.bgm_03,
        R.drawable.bgm_04,
        R.drawable.bgm_05,
        R.drawable.bgm_06,
        R.drawable.bgm_07,
        R.drawable.bgm_08,
        R.drawable.bgm_09,
        R.drawable.bgm_10,
        R.drawable.bgm_11,
        R.drawable.bgm_12,
        R.drawable.bgm_13,
        R.drawable.bgm_14,
        R.drawable.bgm_15,
        R.drawable.bgm_16,
        R.drawable.bgm_17,
        R.drawable.bgm_18,
        R.drawable.bgm_19,
        R.drawable.bgm_20,
        R.drawable.bgm_21,
        R.drawable.bgm_22,
        R.drawable.bgm_23,
        R.drawable.tv_01,
        R.drawable.tv_02,
        R.drawable.tv_03,
        R.drawable.tv_04,
        R.drawable.tv_05,
        R.drawable.tv_06,
        R.drawable.tv_07,
        R.drawable.tv_08,
        R.drawable.tv_09,
        R.drawable.tv_10,
        R.drawable.tv_11,
        R.drawable.tv_12,
        R.drawable.tv_13,
        R.drawable.tv_14,
        R.drawable.tv_15,
        R.drawable.tv_16,
        R.drawable.tv_17,
        R.drawable.tv_18,
        R.drawable.tv_19,
        R.drawable.tv_20,
        R.drawable.tv_21,
        R.drawable.tv_22,
        R.drawable.tv_23,
        R.drawable.tv_24,
        R.drawable.tv_25,
        R.drawable.tv_26,
        R.drawable.tv_27,
        R.drawable.tv_28,
        R.drawable.tv_29,
        R.drawable.tv_30,
        R.drawable.tv_31,
        R.drawable.tv_32,
        R.drawable.tv_33,
        R.drawable.tv_34,
        R.drawable.tv_35,
        R.drawable.tv_36,
        R.drawable.tv_37,
        R.drawable.tv_38,
        R.drawable.tv_39,
        R.drawable.tv_40,
        R.drawable.tv_41,
        R.drawable.tv_42,
        R.drawable.tv_43,
        R.drawable.tv_44,
        R.drawable.tv_45,
        R.drawable.tv_46,
        R.drawable.tv_47,
        R.drawable.tv_48,
        R.drawable.tv_49,
        R.drawable.tv_50,
        R.drawable.tv_51,
        R.drawable.tv_52,
        R.drawable.tv_53,
        R.drawable.tv_54,
        R.drawable.tv_55,
        R.drawable.tv_56,
        R.drawable.tv_57,
        R.drawable.tv_58,
        R.drawable.tv_59,
        R.drawable.tv_60,
        R.drawable.tv_61,
        R.drawable.tv_62,
        R.drawable.tv_63,
        R.drawable.tv_64,
        R.drawable.tv_65,
        R.drawable.tv_66,
        R.drawable.tv_67,
        R.drawable.tv_68,
        R.drawable.tv_69,
        R.drawable.tv_70,
        R.drawable.tv_71,
        R.drawable.tv_72,
        R.drawable.tv_73,
        R.drawable.tv_74,
        R.drawable.tv_75,
        R.drawable.tv_76,
        R.drawable.tv_77,
        R.drawable.tv_78,
        R.drawable.tv_79,
        R.drawable.tv_80,
        R.drawable.tv_81,
        R.drawable.tv_82,
        R.drawable.tv_83,
        R.drawable.tv_84,
        R.drawable.tv_85,
        R.drawable.tv_86,
        R.drawable.tv_87,
        R.drawable.tv_88,
        R.drawable.tv_89,
        R.drawable.tv_90,
        R.drawable.tv_91,
        R.drawable.tv_92,
        R.drawable.tv_93,
        R.drawable.tv_94,
        R.drawable.tv_95,
        R.drawable.tv_96,
        R.drawable.tv_97,
        R.drawable.tv_98,
        R.drawable.tv_99,
        R.drawable.tv_100,
        R.drawable.tv_101,
        R.drawable.tv_102,
)

@DrawableRes
fun bangumiEmoteDrawable(number: Int): Int {
    val idx = number - 1
    return if (idx in STICKER_RES.indices) STICKER_RES[idx] else 0
}

fun containsBangumiEmote(text: String): Boolean = EMOTE_REGEX.containsMatchIn(text)

fun buildBangumiEmoteAnnotatedString(
    text: String,
    emoteSize: TextUnit = EMOTE_SIZE_SP.sp,
): AnnotatedString {
    if (!containsBangumiEmote(text)) return AnnotatedString(text)
    val builder = AnnotatedString.Builder()
    val inline = mutableMapOf<String, InlineTextContent>()
    var last = 0
    var idx = 0
    EMOTE_REGEX.findAll(text).forEach { match ->
        val number = match.groupValues[1].toIntOrNull()
        val res = number?.let { bangumiEmoteDrawable(it) }.takeIf { it != null && it != 0 }
        builder.append(text.substring(last, match.range.first))
        if (res != null) {
            val id = "emote_$idx"
            inline[id] = InlineTextContent(
                placeholder = Placeholder(
                    width = emoteSize,
                    height = emoteSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                Image(
                    painter = painterResource(res),
                    contentDescription = "(bgm$number)",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            builder.appendInlineContent(id)
            idx++
        } else {
            builder.append(match.value)
        }
        last = match.range.last + 1
    }
    builder.append(text.substring(last))
    return builder.toAnnotatedString()
}

@Composable
fun BangumiEmoteText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    androidx.compose.material3.Text(
        text = buildBangumiEmoteAnnotatedString(text),
        style = style,
        modifier = modifier,
    )
}
