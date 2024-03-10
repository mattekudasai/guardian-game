package com.yufimtsev.guardian.player

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.yufimtsev.guardian.GuardianGame.Companion.HUD_VIRTUAL_HEIGHT
import com.yufimtsev.guardian.GuardianGame.Companion.VIRTUAL_WIDTH
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import ktx.graphics.use
import kotlin.math.min

class Hud(
    private val screenWidth: () -> Int,
    private val virtualScreenOffsetX: () -> Int,
    private val virtualScreenOffsetY: () -> Int,
    private val virtualPixelSize: () -> Int,
    private val onHunger: () -> Unit,
) : Disposing by Self() {

    var stamina = 0.5f
        private set

    private var staminaIncreasedCountdown = 0f
    private var staminaDecreasedCountdown = 0f
    private var nothingHappensTimer = 0f

    private val renderer: ShapeRenderer by remember { ShapeRenderer() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(
        VIRTUAL_WIDTH.units,
        HUD_VIRTUAL_HEIGHT.units,
        camera
    )

    private val colorRed = Color.RED
    private val colorGreen = Color.GREEN
    private val colorWhite = Color.WHITE
    private val colorGray = Color.GRAY

    fun processKeyDown(keycode: Int) {
    }

    var negativeEffectAcceleration: Float = 0f

    fun update(delta: Float, isPlayerInWater: Boolean, isPlayerInLostWoods: Boolean) {
        if (staminaDecreasedCountdown > 0f) {
            staminaDecreasedCountdown -= delta
        }
        if (staminaIncreasedCountdown > 0f) {
            staminaIncreasedCountdown -= delta
        }
        nothingHappensTimer += delta
        if (nothingHappensTimer > STAMINA_DECREASE_PERIOD_SECONDS) {
            onHunger()
            decreaseStamina(STAMINA_DECREASE_RATE)
            nothingHappensTimer = 0f
        }
        if (isPlayerInWater || isPlayerInLostWoods) {
            negativeEffectAcceleration += if (isPlayerInWater) 0.00001f else 0.000001f
            decreaseStamina(negativeEffectAcceleration)
        } else {
            negativeEffectAcceleration = 0f
        }
    }

    fun render() {
        val staminaColor = if (staminaDecreasedCountdown > 0f) {
            colorRed
        } else if (staminaIncreasedCountdown > 0f) {
            colorGreen
        } else {
            colorWhite
        }
        viewport.setScreenBounds(
            virtualScreenOffsetX(),
            virtualScreenOffsetY(),
            (VIRTUAL_WIDTH * virtualPixelSize()).toInt(),
            (HUD_VIRTUAL_HEIGHT * virtualPixelSize()).toInt()
        )
        viewport.apply(true)
        renderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.color = Color.BLACK
            it.rect(0f, 0f, VIRTUAL_WIDTH.units, HUD_VIRTUAL_HEIGHT.units)
            it.drawTwoRects(
                1f,
                VIRTUAL_WIDTH,
                2f,
                stamina,
                staminaColor,
                colorGray
            )
        }
    }

    private fun ShapeRenderer.drawTwoRects(
        from: Float,
        to: Float,
        padding: Float,
        fraction: Float,
        colorLeft: Color,
        colorRight: Color
    ) {
        val maxLength = to - from - padding * 2
        val height = HUD_VIRTUAL_HEIGHT - padding * 2
        val firstRectStart = from + padding
        val firstRectEnd = from + padding + maxLength * fraction
        val firstRectWidth = firstRectEnd - firstRectStart
        val secondRectStart = from + padding + maxLength * fraction
        val secondRectEnd = to - padding
        val secondRectWidth = min(secondRectEnd - secondRectStart, maxLength - 1)

        color = colorLeft
        rect(firstRectStart.units.pixels, padding.units.pixels, firstRectWidth.units.pixels, height.units.pixels)
        color = colorRight
        rect(secondRectStart.units.pixels, padding.units.pixels, secondRectWidth.units.pixels, height.units.pixels)
    }

    fun decreaseStamina(value: Float) {
        nothingHappensTimer = 0f
        stamina -= value
        if (stamina <= 0f) {
            stamina = 0f
            // TODO: game over
            return
        }
        staminaIncreasedCountdown = 0f
        staminaDecreasedCountdown = DEFAULT_COUNTDOWN_SECONDS
    }

    fun increaseStamina(value: Float) {
        nothingHappensTimer = 0f
        if (stamina == 1f) {
            return
        }
        stamina += value
        if (stamina > 1f) {
            stamina = 1f
        }
        staminaDecreasedCountdown = 0f
        staminaIncreasedCountdown = DEFAULT_COUNTDOWN_SECONDS
    }

    fun reset() {
        stamina = 0.5f
        nothingHappensTimer = 0f
        staminaIncreasedCountdown = 0f
        staminaDecreasedCountdown = 0f
        nothingHappensTimer = 0f
    }

    companion object {
        private const val DEFAULT_COUNTDOWN_SECONDS = 0.3f
        private const val STAMINA_DECREASE_PERIOD_SECONDS = 4f
        private const val STAMINA_DECREASE_RATE = 0.01f
    }
}
