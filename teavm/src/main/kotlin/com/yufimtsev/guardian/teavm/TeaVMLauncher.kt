@file:JvmName("TeaVMLauncher")

package com.yufimtsev.guardian.teavm

import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import com.github.xpenatan.gdx.backends.teavm.TeaApplication
import com.yufimtsev.guardian.GuardianGame

/** Launches the TeaVM/HTML application. */
fun main() {
    val config = TeaApplicationConfiguration("canvas").apply {
        width = 0
        height = 0
    }
    TeaApplication(GuardianGame(60, {
    }), config)
}
