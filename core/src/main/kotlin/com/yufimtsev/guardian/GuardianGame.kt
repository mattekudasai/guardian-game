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
//        const val VIRTUAL_WIDTH = 128f
//        const val VIRTUAL_HEIGHT = 120f
        const val MAX_PLAYER_VELOCITY = 1f
        const val PLAYER_ACCELERATION = 0.1f
//        const val VIRTUAL_WIDTH = 1280f
//        const val VIRTUAL_HEIGHT = 768f
    }
}
