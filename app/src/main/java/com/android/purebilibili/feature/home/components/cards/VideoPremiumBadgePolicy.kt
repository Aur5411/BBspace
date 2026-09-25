package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.data.model.response.VideoRights

internal fun resolveVideoPremiumBadgeLabel(
    rights: VideoRights?
): String? {
    if (rights == null) return null
    if (rights.ugcPay > 0 || rights.ugcPayPreview > 0) return "充电专属"
    if (rights.pay > 0 || rights.arcPay > 0) return "付费"
    return null
}
