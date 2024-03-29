package com.yufimtsev.guardian.player

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import com.yufimtsev.guardian.GuardianGame.Companion.BLOCK_BIT
import com.yufimtsev.guardian.GuardianGame.Companion.FIREBALL_BIT
import com.yufimtsev.guardian.GuardianGame.Companion.MAX_PLAYER_RUNNING_VELOCITY
import com.yufimtsev.guardian.GuardianGame.Companion.MAX_PLAYER_VELOCITY
import com.yufimtsev.guardian.GuardianGame.Companion.NIGHT_BIT
import com.yufimtsev.guardian.GuardianGame.Companion.NIGHT_CRASH_BIT
import com.yufimtsev.guardian.GuardianGame.Companion.PLAYER_ACCELERATION
import com.yufimtsev.guardian.GuardianGame.Companion.PLAYER_BIT
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import com.yufimtsev.guardian.world.Fireball
import ktx.collections.GdxArray
import kotlin.experimental.or

class Player(world: World, texture: Texture, private val spawnPosition: Vector2) :
    Sprite(texture), Disposing by Self() {

    private val idleTexture = TextureRegion(texture, 0, 0, 16, 16)
    private val runAnimation = Animation(0.1f, GdxArray(Array(4) { TextureRegion(texture, it * 16, 0, 16, 16) }))
    private val halfOfAnimation = runAnimation.animationDuration / 2f
    private val stepLeft: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("step_left.wav")) }
    private val stepRight: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("step_right.wav")) }
    private val jumpSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("jump.wav")) }
    private val fallSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("fall.wav")) }

    val body: Body = world.createBody(BodyDef().apply {
        position.set(spawnPosition.x.units, spawnPosition.y.units)
        type = BodyDef.BodyType.DynamicBody
    }).apply {
        createFixture(FixtureDef().apply {
            shape = PolygonShape().apply {
                setAsBox(
                    3f.units,
                    8f.units
                )
            }
            /*shape = CircleShape().apply {
                radius = 8f.units
            }*/
            friction = 0.8f
            filter.categoryBits = PLAYER_BIT
            filter.maskBits = NIGHT_BIT or BLOCK_BIT or NIGHT_CRASH_BIT or FIREBALL_BIT
        }).apply {
            userData = this@Player
        }
    }

    private val currentState: State
        get() = with(body.linearVelocity) {
            when {
                x == 0f -> State.IDLE
                else -> State.RUNNING
            }
        }
    private var previousState: State = State.IDLE
    private var runningRight: Boolean = true
    private var stateTime: Float = 0f
    private var nextStep: Float = 0f
    private var nextStepRight: Boolean = false
    private var wasJumping: Boolean = false
    private var wasFalling: Boolean = false

    init {
        setBounds(0f, 0f, 16f.units, 16f.units)
        setRegion(idleTexture)
    }

    fun update(delta: Float, isInWater: Boolean, shouldIgnoreInputs: Boolean, refreshRate: Int) {
        if (!shouldIgnoreInputs) {
            handleInput(delta, isInWater, refreshRate)
        }
        setPosition((body.position.x - width / 2).pixels, (body.position.y - height / 2).pixels)
        setRegion(getFrame(delta))
    }

    private var jumped: Boolean = false

    private fun handleInput(delta: Float, isInWater: Boolean, refreshRate: Int) {
        // TODO: switch to event-based handling
        val multiplier = if (refreshRate < 60) 60f / refreshRate else 1f
        val holdingShift =
            Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) ||
                Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT) ||
                Gdx.input.isKeyPressed(Keys.SPACE) ||
                Gdx.input.isKeyPressed(Keys.K) ||
                Gdx.input.isKeyPressed(Keys.Z)
        if ((Gdx.input.isKeyJustPressed(Keys.UP) || Gdx.input.isKeyJustPressed(Keys.W) || Gdx.input.isKeyJustPressed(
                Keys.J
            ) || Gdx.input.isKeyJustPressed(
                Keys.X
            )) && body.linearVelocity.y == 0f && !jumped
        ) {
            jumped = true
            body.applyLinearImpulse(Vector2(0f, if (isInWater) 1f else 2f), body.worldCenter, true)
        }
        if (jumped && !(Gdx.input.isKeyPressed(Keys.UP) || Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.J) || Gdx.input.isKeyPressed(
                Keys.X
            ))
        ) {
            jumped = false
        }
        val maxVelocity = if (holdingShift) MAX_PLAYER_RUNNING_VELOCITY else MAX_PLAYER_VELOCITY
        val realMaxVelocity = if (isInWater) maxVelocity / 2f else maxVelocity
        if ((Gdx.input.isKeyPressed(Keys.RIGHT) || Gdx.input.isKeyPressed(Keys.D)) && body.linearVelocity.x <= realMaxVelocity) {
            body.applyLinearImpulse(Vector2(PLAYER_ACCELERATION * multiplier, 0f), body.worldCenter, true)
        }
        if ((Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)) && body.linearVelocity.x >= -realMaxVelocity) {
            body.applyLinearImpulse(Vector2(-PLAYER_ACCELERATION * multiplier, 0f), body.worldCenter, true)
        }
    }


    private fun getFrame(delta: Float): TextureRegion {
        val currentState = currentState
        val result = when (currentState) {
            State.RUNNING -> runAnimation.getKeyFrame(stateTime, true)
            State.IDLE -> idleTexture
        }

        if (currentState == State.RUNNING) {
            runningRight = body.linearVelocity.x > 0f
        }

        if (runningRight == result.isFlipX) {
            result.flip(true, false)
        }

        if (!wasJumping && body.linearVelocity.y > 0f) {
            wasJumping = true
            jumpSound.play()
        } else if (body.linearVelocity.y <= 0f) {
            wasJumping = false
        }


        if (body.linearVelocity.y < 0f) {
            wasFalling = true
        } else if (wasFalling && body.linearVelocity.y == 0f) {
            wasFalling = false
            fallSound.play()
        }

        stateTime = if (currentState == previousState) stateTime + delta * Math.abs(body.linearVelocity.x) else 0f
        if (currentState == previousState) {
            val deltaAdjustedForSpeed = delta * Math.abs(body.linearVelocity.x)
            stateTime += deltaAdjustedForSpeed
            nextStep += deltaAdjustedForSpeed
            if (nextStep >= halfOfAnimation) {
                nextStep -= halfOfAnimation
                if (body.linearVelocity.y == 0f) {
                    if (nextStepRight) {
                        stepRight.play()
                    } else {
                        stepLeft.play()
                    }
                }
                nextStepRight = !nextStepRight
            }
        } else {
            stateTime = 0f
            nextStep = 0f
        }
        previousState = currentState

        return result
    }

    private enum class State { IDLE, RUNNING }

}

