package com.yufimtsev.guardian.player

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import com.yufimtsev.guardian.GuardianGame.Companion.MAX_PLAYER_VELOCITY
import com.yufimtsev.guardian.GuardianGame.Companion.PLAYER_ACCELERATION
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import ktx.collections.GdxArray

class Player(world: World, texture: Texture, private val spawnPosition: Vector2) :
    Sprite(texture), Disposing by Self() {

    private val idleTexture = TextureRegion(texture, 0, 0, 16, 16)
    private val runAnimation = Animation(0.1f, GdxArray(Array(4) { TextureRegion(texture, it * 16, 0, 16, 16) }))

    val body: Body = world.createBody(BodyDef().apply {
        position.set(spawnPosition.x.units, spawnPosition.y.units)
        type = BodyDef.BodyType.DynamicBody
    }).apply {
        createFixture(FixtureDef().apply {
            shape = CircleShape().apply {
                radius = 8f.units
            }
            friction = 0.8f
        })
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

    init {
        setBounds(0f, 0f, 16f.units, 16f.units)
        setRegion(idleTexture)
    }

    fun update(delta: Float) {
        handleInput()
        setPosition((body.position.x - width / 2).pixels, (body.position.y - height / 2).pixels)
        setRegion(getFrame(delta))
    }

    private fun handleInput() {
        // TODO: switch to event-based handling
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            body.applyLinearImpulse(Vector2(0f, 2f), body.worldCenter, true)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) && body.linearVelocity.x <= MAX_PLAYER_VELOCITY) {
            body.applyLinearImpulse(Vector2(PLAYER_ACCELERATION, 0f), body.worldCenter, true)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) && body.linearVelocity.x >= -MAX_PLAYER_VELOCITY) {
            body.applyLinearImpulse(Vector2(-PLAYER_ACCELERATION, 0f), body.worldCenter, true)
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

        stateTime = if (currentState == previousState) stateTime + delta else 0f
        previousState = currentState

        return result
    }

    private enum class State { IDLE, RUNNING }

}

