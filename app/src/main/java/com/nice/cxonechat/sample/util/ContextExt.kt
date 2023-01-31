package com.nice.cxonechat.sample.util

import android.content.Context
import android.util.TypedValue

/**
 * Convert DP to Pixels in the receiving context.
 *
 * @receiver Context for display metrics
 * @param dp DP to convert to pixels
 * @return converted pixels
 */
internal fun Context.dpToPixels(dp: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
