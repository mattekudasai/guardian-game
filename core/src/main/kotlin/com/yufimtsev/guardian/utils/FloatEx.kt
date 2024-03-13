package com.yufimtsev.guardian.utils

import com.yufimtsev.guardian.GuardianGame.Companion.PIXELS_PER_UNIT
import kotlin.math.floor

inline val Float.units
    get() = this / PIXELS_PER_UNIT

var pixelAligned: Boolean = true

inline val Float.pixels
    get() = if (pixelAligned) {
        floor(this * PIXELS_PER_UNIT) / PIXELS_PER_UNIT
    } else {
        this
    }
