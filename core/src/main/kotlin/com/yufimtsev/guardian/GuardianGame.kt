@file:Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")

package com.yufimtsev.guardian

import ktx.app.KtxGame
import ktx.app.KtxScreen

class GuardianGame(private val refreshRateLimit: Int, private val toggleFullScreen: () -> Unit, private val debugRenderer: Boolean = false) : KtxGame<KtxScreen>() {
    override fun create() {
        addScreen(GameScreen(refreshRateLimit, toggleFullScreen, debugRenderer))
        setScreen<GameScreen>()
    }

    companion object {
        const val PIXELS_PER_UNIT = 100f
        const val VIRTUAL_WIDTH = 256f
        const val VIRTUAL_HEIGHT = 240f
        const val CHECK_VIRTUAL_WIDTH = 64f
        const val CHECK_VIRTUAL_HEIGHT = 64f
        const val HUD_VIRTUAL_HEIGHT = 16f
        const val MAX_PLAYER_VELOCITY = 1f
        const val MAX_PLAYER_RUNNING_VELOCITY = 2f
        const val PLAYER_ACCELERATION = 0.1f


        const val PLAYER_BIT: Short = 0b1
        const val BLOCK_BIT: Short = 0b10
        const val NIGHT_BIT: Short = 0b100
        const val NIGHT_CRASH_BIT: Short = 0b1000
        const val FIREBALL_BIT: Short = 0b10000
    }
}
