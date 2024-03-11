package com.yufimtsev.guardian.utils

import com.yufimtsev.guardian.GuardianGame.Companion.PIXELS_PER_UNIT

inline val Float.units
    get() = this / PIXELS_PER_UNIT

var pixelAligned: Boolean = true

inline val Float.pixels
    get() = if (pixelAligned) {
        (this * PIXELS_PER_UNIT).toInt() / PIXELS_PER_UNIT
    } else {
        this
    }
