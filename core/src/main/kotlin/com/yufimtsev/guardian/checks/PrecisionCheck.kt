package com.yufimtsev.guardian.checks

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.yufimtsev.guardian.GuardianGame
import com.yufimtsev.guardian.GuardianGame.Companion.CHECK_VIRTUAL_HEIGHT
import com.yufimtsev.guardian.GuardianGame.Companion.CHECK_VIRTUAL_WIDTH
import com.yufimtsev.guardian.GuardianGame.Companion.VIRTUAL_HEIGHT
import com.yufimtsev.guardian.GuardianGame.Companion.VIRTUAL_WIDTH
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
    private val actionDifference: (Float) -> Unit,
    private val actionStopped: (userMovedAway: Boolean) -> Unit,
) :
    Disposing by Self() {

    private val renderer: ShapeRenderer by remember { ShapeRenderer() }
    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }
    private val camera = OrthographicCamera()
    private var viewport = FitViewport(
        CHECK_VIRTUAL_WIDTH.units,
        CHECK_VIRTUAL_HEIGHT.units,
        camera
    )

    var showing: Boolean = false
    private var isGameEnding: Boolean = false
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
    private var onActionCallback: (position: Float, delta: Float) -> Unit = { _, _ -> }
    private var onFinishCallback: (position: Float, delta: Float) -> Unit = { _, _ -> }
    var isFullscreen = false
    private var tint: Float = 0f
    private var progress: Float = 0f
    private var timeLimit: Float = 0f

    private val powerTexture: Texture by remember { Texture("power.png") }
    private val buzzSound = Gdx.audio.newSound(Gdx.files.internal("buzz.wav")).autoDisposing()
    private var buzzSoundId: Long = -2L

    private val centerDelta: Float?
        get() = fixedOnPosition?.let { Math.abs(target - it) }

    fun processKeyDown(keycode: Int): Boolean {
        // ignore input if check is finished
        if (fixedOnPosition != null || !showing) {
            return false
        }
        if (keycode == Keys.SPACE || keycode == Keys.SHIFT_LEFT || keycode == Keys.SHIFT_RIGHT || keycode == Keys.K || keycode == Keys.Z || keycode == Keys.J || keycode == Keys.X) {
            val currentPosition = guidePosition
            fixedOnPosition = currentPosition
            timer = 0f
            centerDelta?.let {
                actionDifference(it)
                onActionCallback(currentPosition, it)
            }
            actionStopped(false)
            return true
        }
        if (!isFullscreen && !isGameEnding) {
            if (keycode == Keys.UP || keycode == Keys.W || keycode == Keys.LEFT || keycode == Keys.RIGHT || keycode == Keys.A || keycode == Keys.D) {
                // just ignore that
                /*showing = false
                actionStopped(true)*/
                return true
            }
        }
        return false
    }

    fun updateAndRender(delta: Float, isGameEnding: Boolean) {
        this.isGameEnding = isGameEnding
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
            buzzSound.stop()
            if (buzzSoundId != -2L) {
                buzzSoundId = -2L
            }
            if (timer > disappearIn) {
                showing = false
                onFinishCallback(fixedOnPosition!!, centerDelta!!)
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
        val offsetX = (maxWidth.units - worldWidth) / 2f
        val offsetY = (maxHeight.units - worldHeight) / 2f
        if (!isFullscreen) {
            renderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                it.setColor(0f, 0f, 0f, 1f)
                it.rect(offsetX.pixels, offsetY.pixels, worldWidth.pixels, worldHeight.pixels)
            }
        }
        val texture = texture
        if (texture != null) {
            spriteBatch.use(camera) {
                it.draw(texture, offsetX.pixels, offsetY.pixels, worldWidth.pixels, worldHeight.pixels)
            }
        }
        if (!appearing) {
            if (fixedOnPosition == null) {
                if (buzzSoundId == -2L) {
                    buzzSound.stop()
                    buzzSoundId = buzzSound.play() // calling .loop() breaks playback on webgl
                    println("buzz started: $buzzSoundId")
                } else {
                    buzzSound.setLooping(buzzSoundId, true)
                    buzzSound.setPitch(buzzSoundId, position / 2f + 1f)
                }
            }
            renderer.use(ShapeRenderer.ShapeType.Filled, camera) {
                if (showGuide) {
                    it.setColor(0.5f, 1f, 0.5f, 1f)
                    val xPosition = if (isVertical) worldWidth * (1f + target) / 2 else 0f
                    val yPosition = if (isVertical) 0f else worldHeight * (1f + target) / 2
                    val rectWidth = if (isVertical) 2f.units else worldWidth
                    val rectHeight = if (isVertical) worldHeight else 2f.units
                    it.rect(xPosition.pixels, yPosition.pixels, rectWidth.pixels, rectHeight.pixels)
                }
                val currentPosition = powerForFixedPosition ?: position
                it.setColor(1f, 1f, 1f, 1f)
                val xPosition = if (isVertical) worldWidth * (1f + currentPosition) / 2 else 0f
                val yPosition = if (isVertical) 0f else worldHeight * (1f + currentPosition) / 2
                val rectWidth = if (isVertical) 2f.units else worldWidth
                val rectHeight = if (isVertical) worldHeight else 2f.units
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
        texture: Texture?,
        target: Float,
        isVertical: Boolean,
        powerForFixedPosition: Float? = null,
        speed: Float = 5f,
        disappearIn: Float = SECONDS_TO_DISAPPEAR,
        showGuide: Boolean = false,
        appearing: Boolean = false,
        isFullscreen: Boolean = false,
        tint: Float = 0f,
        progress: Float = 0f,
        timeLimit: Float = -1f,
        onActionCallback: (Float, Float) -> Unit = { _, _ -> },
        onFinishCallback: (Float, Float) -> Unit = { _, _ -> },
    ) {
        this.texture = texture
        this.target = target
        this.isVertical = isVertical
        this.powerForFixedPosition = powerForFixedPosition
        this.appearing = appearing
        this.onActionCallback = onActionCallback
        this.onFinishCallback = onFinishCallback
        this.speed = speed
        this.disappearIn = disappearIn
        this.isFullscreen = isFullscreen
        this.tint = tint
        this.progress = progress
        this.timeLimit = timeLimit
        timer = 0f
        fixedOnPosition = null
        this.showGuide = showGuide
        viewport = FitViewport(
            maxWidth.units,
            maxHeight.units,
            camera
        )
        showing = true
    }


    private val maxWidth get() = if (isFullscreen) VIRTUAL_WIDTH else CHECK_VIRTUAL_WIDTH
    private val maxHeight get() = if (isFullscreen) VIRTUAL_HEIGHT else CHECK_VIRTUAL_HEIGHT

    private fun updateViewport(fraction: Float) {
        val width = (maxWidth * fraction)
        val height = (maxHeight * fraction)
        with(viewport) {
            val virtualPixelSize = virtualPixelSize()
            val x = ((screenWidth() - width * virtualPixelSize) / 2).pixels.toInt()
            val y = (maxHeight.units - height).pixels.toInt() / 2 + 63 + (64 * fraction).toInt()
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

    fun reset() {
        showing = false
    }

    // in range of -1f..1f
    private val guidePosition: Float
        get() = sin(timer * speed + 3f * Math.PI / 2.0).toFloat()


    companion object {
        private const val SECONDS_TO_APPEAR = 0.3f
        private const val SECONDS_TO_DISAPPEAR = 1f
    }
}
