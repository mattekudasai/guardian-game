package com.yufimtsev.guardian.checks

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.yufimtsev.guardian.GuardianGame.Companion.CHECK_VIRTUAL_HEIGHT
import com.yufimtsev.guardian.GuardianGame.Companion.CHECK_VIRTUAL_WIDTH
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import ktx.graphics.use
import kotlin.math.min
import kotlin.math.sin

class PrecisionCheck(
    private val virtualPixelSize: () -> Int,
    private val virtualScreenOffsetY: () -> Int,
    private val screenWidth: () -> Int,
    private val actionDifference: (Float) -> Unit
) :
    Disposing by Self() {

    private val renderer: ShapeRenderer by remember { ShapeRenderer() }
    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(
        CHECK_VIRTUAL_WIDTH.units,
        CHECK_VIRTUAL_HEIGHT.units,
        camera
    )

    private var showing: Boolean = false
    private var appearing: Boolean = true
    private var disappearIn: Float = SECONDS_TO_DISAPPEAR
    private var timer: Float = 0f

    // after player pressed the button this is filled
    // if null - the check is in progress
    private var fixedOnPosition: Float? = null

    // changes the rendering line orientation
    private var isVertical: Boolean = false
    private var showGuide: Boolean = false
    private var powerForFixedPosition: Float? = null
    private var speed: Float = 5f
    private var target: Float = 0f
    private var texture: Texture? = null
    private var onFinishCallback: (Float) -> Unit = {}

    private val powerTexture: Texture by remember { Texture("power.png") }

    private val centerDelta: Float?
        get() = fixedOnPosition?.let { Math.abs(target - it) }

    fun processKeyDown(keycode: Int) {
        // ignore input if check is finished
        if (fixedOnPosition != null) {
            return
        }
        if (keycode == Keys.SPACE) {
            fixedOnPosition = guidePosition
            timer = 0f
            centerDelta?.let { actionDifference(it) }
        }
    }

    fun updateAndRender(delta: Float) {
        if (!showing) {
            return
        }
        timer += delta

        val position = if (appearing) {
            val appearedFraction = min(1f, timer / SECONDS_TO_APPEAR)
            updateViewport(appearedFraction)
            if (appearedFraction == 1f) {
                appearing = false
                timer = 0f
            }
            guidePosition
        } else if (fixedOnPosition != null) {
            if (timer > disappearIn) {
                showing = false
                onFinishCallback(fixedOnPosition!!)
            }
            updateViewport(1f)
            fixedOnPosition ?: guidePosition
        } else {
            updateViewport(1f)
            guidePosition
        }

        viewport.apply()
        val worldWidth = viewport.worldWidth
        val worldHeight = viewport.worldHeight
        val offsetX = (CHECK_VIRTUAL_WIDTH.units - worldWidth) / 2f
        val offsetY = (CHECK_VIRTUAL_HEIGHT.units - worldHeight) / 2f
        renderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.setColor(0f, 0f, 0f, 1f)
            it.rect(offsetX.pixels, offsetY.pixels, worldWidth.pixels, worldHeight.pixels)
        }
        val texture = texture
        if (texture != null) {
            spriteBatch.use(camera) {
                it.draw(texture, offsetX.pixels, offsetY.pixels, worldWidth.pixels, worldHeight.pixels)
            }
        }
        if (!appearing) {
            renderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                if (showGuide) {
                    it.setColor(0.5f, 1f, 0.5f, 1f)
                    val xPosition = if (isVertical) worldWidth * (1f + target) / 2 else 0f
                    val yPosition = if (isVertical) 0f else worldHeight * (1f + target) / 2
                    val rectWidth = if (isVertical) 1f.units else worldWidth
                    val rectHeight = if (isVertical) worldHeight else 1f.units
                    it.rect(xPosition.pixels, yPosition.pixels, rectWidth.pixels, rectHeight.pixels)
                }
                val currentPosition = powerForFixedPosition ?: position
                it.setColor(1f, 1f, 1f, 1f)
                val xPosition = if (isVertical) worldWidth * (1f + currentPosition) / 2 else 0f
                val yPosition = if (isVertical) 0f else worldHeight * (1f + currentPosition) / 2
                val rectWidth = if (isVertical) 1f.units else worldWidth
                val rectHeight = if (isVertical) worldHeight else 1f.units
                it.rect(xPosition.pixels, yPosition.pixels, rectWidth.pixels, rectHeight.pixels)
            }
            if (powerForFixedPosition != null) {
                val normalizedPosition = (position + 1f) / 2f
                spriteBatch.use(camera) {
                    it.draw(
                        powerTexture,
                        offsetX.pixels,
                        offsetY.pixels,
                        worldWidth.pixels,
                        (worldHeight * normalizedPosition).pixels,
                        0,
                        64 - (64 * normalizedPosition).toInt(),
                        64,
                        (64 * normalizedPosition + 1).toInt(),
                        false,
                        false
                    )
                }
            }
        }
    }

    fun show(
        texture: Texture,
        target: Float,
        isVertical: Boolean,
        powerForFixedPosition: Float?,
        speed: Float = 5f,
        disappearIn: Float = SECONDS_TO_DISAPPEAR,
        showGuide: Boolean = false,
        appearing: Boolean = false,
        onFinishCallback: (Float) -> Unit = {}
    ) {
        this.texture = texture
        this.target = target
        this.isVertical = isVertical
        this.powerForFixedPosition = powerForFixedPosition
        this.appearing = appearing
        this.onFinishCallback = onFinishCallback
        this.speed = speed
        this.disappearIn = disappearIn
        timer = 0f
        fixedOnPosition = null
        this.showGuide = showGuide
        showing = true
    }

    private fun updateViewport(fraction: Float) {
        val width = (CHECK_VIRTUAL_WIDTH * fraction)
        val height = (CHECK_VIRTUAL_HEIGHT * fraction)
        with(viewport) {
            val virtualPixelSize = virtualPixelSize()
            val x = ((screenWidth() - width * virtualPixelSize) / 2).pixels.toInt()
            val y = (CHECK_VIRTUAL_HEIGHT.units - height).pixels.toInt() / 2 + 63 + (64 * fraction).toInt()
            val w = width.pixels.toInt()
            val h = height.pixels.toInt()
            setScreenBounds(
                x,
                y * virtualPixelSize + virtualScreenOffsetY(),
                w * virtualPixelSize,
                h * virtualPixelSize
            )
            //update(w * virtualPixelSize, h * virtualPixelSize)
            apply(true)
        }
    }

    // in range of -1f..1f
    private val guidePosition: Float
        get() = sin(timer * speed + 3f * Math.PI / 2.0).toFloat()


    companion object {
        private const val SECONDS_TO_APPEAR = 0.3f
        private const val SECONDS_TO_DISAPPEAR = 1f
    }
}