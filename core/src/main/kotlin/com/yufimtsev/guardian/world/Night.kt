package com.yufimtsev.guardian.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.EdgeShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import com.yufimtsev.guardian.GuardianGame
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import ktx.collections.GdxArray
import kotlin.math.max
import kotlin.math.sin

class Night(
    world: World,
    private val spawnPosition: Vector2,
    private val onHeadDown: (Night) -> Unit,
    private val onHeadUp: () -> Unit,
    private val spawnFireball: (position: Vector2, goingRight: Boolean) -> Unit,
) : Sprite(), Disposing by Self() {

    private val idleTexture: Texture by remember { Texture("night/idle.png") }
    private val bodyTexture: Texture by remember { Texture("night/body.png") }
    private val attackTextures = Array(5) {
        Texture("night/attack_${it + 1}.png").autoDisposing()
    }
    private val attackAnimationSequence = listOf(4, 3, 2, 1, 0, 1, 2, 3, 4)
    private val pickTextures = Array(3) {
        Texture("night/pick_${it + 1}.png").autoDisposing()
    }
    private val flyTextures = Array(9) {
        Texture("night/fly_${it + 1}.png").autoDisposing()
    }
    private val flyAnimationSequence = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4)

    private val attackAnimation = Animation(0.1f, GdxArray(Array(attackAnimationSequence.size) {
        attackTextures[attackAnimationSequence[it]]
    }))
    private val pickAnimation = Animation(0.1f, GdxArray(Array(15) {
        if (it < 5) {
            attackTextures[it]
        } else if (it < 8) {
            pickTextures[it - 5]
        } else if (it < 10) {
            pickTextures[pickTextures.size - 1]
        } else if (it < 12) {
            pickTextures[11 - it]
        } else {
            idleTexture
        }
    }))
    private val flyAnimation =
        Animation(0.06f, GdxArray(Array(flyAnimationSequence.size) { flyTextures[flyAnimationSequence[it]] }))

    var health = 2.5f
    val body: Body = world.createBody(BodyDef().apply {
        position.set(spawnPosition.x.units, spawnPosition.y.units)
        type = BodyDef.BodyType.DynamicBody
    }).apply {
        val height = (6 * 16f / 558f * 1024f / 4f).units
        val width = (6 * 16f / 2.5f).units
        createFixture(FixtureDef().apply {
            shape = PolygonShape().apply {
                setAsBox(height, width)
            }
            friction = 0f
            filter.categoryBits = GuardianGame.NIGHT_BIT
        }).apply {
            userData = this@Night
        }
        createFixture(FixtureDef().apply {
            shape = EdgeShape().apply {
                set(-width - 4f.units, -height + 8f.units, width + 5f.units, -height + 8f.units)
            }
            filter.categoryBits = GuardianGame.NIGHT_CRASH_BIT
            filter.maskBits = GuardianGame.PLAYER_BIT
        }).apply {
            userData = Fatal
        }
    }

    private val stateSequence = listOf(
        State.INIT to 2f,
        State.IDLE to 2f,
        State.PICKING to pickAnimation.animationDuration,
        State.IDLE to 0.5f,
        State.PICKING to pickAnimation.animationDuration,
        State.IDLE to 0.5f,
        State.PICKING to pickAnimation.animationDuration,
        State.IDLE to 1f,
        State.FLYING_UP to 2f,
        State.FLYING to 1f,
        State.FLYING_LEFT to 2f,
        State.FLYING_RIGHT to 3f,
        State.FLYING_LEFT to 1f,
        State.FLYING to 1f,
        State.FLYING_DOWN to 1f,
    )

    private var currentStateIndex = 0
    private val currentState: State
        get() = stateSequence[currentStateIndex].first
    private var previousState: State = State.IDLE
    private var stateTime: Float = 0f
    private var timeToFreeze = 0f
    private var headDown = false
    private var fireballTimer = 0f
    private var initialSequence = 2f

    init {
        setBounds(0f, 0f, (6 * 16f / 558f * 1024f).units, 6 * 16f.units)
        texture = idleTexture
    }

    fun update(delta: Float, playerPositionX: Float) {
        val frame = getFrame(delta)
        val state = currentState

        val originX = (playerPositionX - GuardianGame.VIRTUAL_WIDTH.units).pixels
        val targetX = (body.position.x - width / 2).pixels
        val diff = targetX - originX
        val timeFraction = stateTime / stateSequence.first().second

        if (state == State.INIT) {

            setPosition((originX + diff * timeFraction).pixels, (body.position.y + 64f.units).pixels)
        } else {
            setPosition((body.position.x - width / 2).pixels, (body.position.y - height / 2).pixels)
        }

        setRegion(frame)
        setFlip(state == State.FLYING_RIGHT || state == State.INIT, false)
        if (state == State.FLYING || state == State.FLYING_LEFT || state == State.FLYING_RIGHT || state == State.INIT) {
            fireballTimer -= delta
            if (fireballTimer <= 0f) {
                if (state == State.INIT) {
                    spawnFireball(
                        Vector2((originX + diff * timeFraction).pixels, (body.position.y + 64f.units).pixels),
                        true
                    )
                    fireballTimer = 0.4f
                } else {
                    val goingRight = state == State.FLYING_RIGHT
                    val offsetX = if (goingRight) 64f else -64f
                    spawnFireball(Vector2(body.position.x + offsetX.units, body.position.y), goingRight)
                    fireballTimer = max(0.2f, health / 2f)
                }
            }
        } else {
            fireballTimer = 0f
        }
    }

    fun freeze(time: Float) {
        timeToFreeze = time
    }

    private fun calculateFlyingPosition(time: Float): Float {
        return sin(time * 1.35f) // 1.35f nicely synchronizes it with animation
    }

    private fun calculatePickingOffset(time: Float): Float {
        return sin(time * 4f + Math.PI).toFloat() // 8f nicely synchronizes it with animation
    }

    private fun getFrame(delta: Float): Texture {
        var adjustedDelta = delta
        if (timeToFreeze > 0f) {
            timeToFreeze -= delta
            if (timeToFreeze < 0f) {
                adjustedDelta = -timeToFreeze
                timeToFreeze = 0f
            } else {
                adjustedDelta = 0f
            }
        } else if (health <= 0f) {
            return bodyTexture
        }
        //return idleTexture
        var currentState = currentState
        stateTime = if (currentState == previousState) stateTime + adjustedDelta else 0f
        if (stateTime > stateSequence[currentStateIndex].second) {
            stateTime -= stateSequence[currentStateIndex].second
            currentStateIndex = (currentStateIndex + 1) % stateSequence.size
            if (currentStateIndex == 0) { // skipping init
                currentStateIndex++
            }
            currentState = stateSequence[currentStateIndex].first
        }

        val result = when (currentState) {
            State.INIT -> flyAnimation.getKeyFrame(stateTime, true)
            State.FLYING -> flyAnimation.getKeyFrame(stateTime, true)
            State.ATTACK -> attackAnimation.getKeyFrame(stateTime, true)
            State.PICKING -> pickAnimation.getKeyFrame(stateTime, true)
            State.IDLE -> idleTexture
            State.FLYING_UP -> flyAnimation.getKeyFrame(stateTime, true)
            State.FLYING_DOWN -> flyAnimation.getKeyFrame(stateTime, true)
            State.FLYING_LEFT -> flyAnimation.getKeyFrame(stateTime, true)
            State.FLYING_RIGHT -> flyAnimation.getKeyFrame(stateTime, true)
            State.ATTACK_IDLE -> attackTextures[attackTextures.size - 1]
        }

        if (currentState == State.FLYING_DOWN || currentState == State.IDLE) {
            body.type = BodyDef.BodyType.DynamicBody
        } else {
            body.type = BodyDef.BodyType.StaticBody
        }

        if (currentState == State.PICKING) {
            if (stateTime > pickAnimation.animationDuration * 3f / 4f) {
                if (headDown) {
                    headDown = false
                    onHeadUp()
                }
            } else if (stateTime > pickAnimation.animationDuration / 4f) {
                if (!headDown) {
                    headDown = true
                    onHeadDown(this)
                }
            }
        } else {
            if (headDown) {
                onHeadUp()
                headDown = false
            }
        }

        if (timeToFreeze == 0f) {
            if (currentState == State.FLYING || currentState == State.FLYING_LEFT || currentState == State.FLYING_RIGHT || currentState == State.FLYING_UP) {
                val xOffset = if (currentState == State.FLYING_LEFT) {
                    -2f * adjustedDelta
                } else if (currentState == State.FLYING_RIGHT) {
                    2f * adjustedDelta
                } else 0f
                val yOffset = if (currentState == State.FLYING_UP) {
                    0.5f * adjustedDelta
                } else {
                    0f * adjustedDelta
                }
                body.setTransform(
                    body.position.x + xOffset,
                    body.position.y + yOffset + calculateFlyingPosition(stateTime) * 0.001f,
                    0f
                )
            } else if (currentState == State.PICKING) {
                body.setTransform(body.position.x + calculatePickingOffset(stateTime) * 0.01f, body.position.y, 0f)
            }
        }

        previousState = currentState

        return result
    }

    val isFrozen: Boolean
        get() = timeToFreeze > 0f


    private enum class State { INIT, IDLE, FLYING_UP, FLYING_DOWN, FLYING_LEFT, FLYING_RIGHT, FLYING, ATTACK, ATTACK_IDLE, PICKING }

    object Fatal

}

