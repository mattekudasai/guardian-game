package com.yufimtsev.guardian.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import com.yufimtsev.guardian.GuardianGame
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import kotlin.experimental.or

class Fireball(world: World, texture: Texture, private val spawnPosition: Vector2, private val goingRight: Boolean) : Sprite(texture) {

    val body: Body = world.createBody(BodyDef().apply {
        position.set(spawnPosition.x, spawnPosition.y)
        type = BodyDef.BodyType.StaticBody
    }).apply {
        createFixture(FixtureDef().apply {
            shape = CircleShape().apply {
                radius = 16f.units
            }
            friction = 0.8f
            filter.categoryBits = GuardianGame.FIREBALL_BIT
            filter.maskBits = GuardianGame.PLAYER_BIT or GuardianGame.BLOCK_BIT
        }).apply {
            userData = this@Fireball
        }
    }
    init {
        setBounds(0f, 0f, 32f.units, 32f.units)
        setRegion(texture)
        setPosition((body.position.x - width / 2).pixels, (body.position.y - height / 2).pixels)
        setFlip(!goingRight, false)
    }

    var isDestroyed = false

    fun updatePosition(delta: Float) {
        if (isDestroyed) {
            return
        }
        val positionX = if (goingRight) {
            body.position.x + VELOCITY * delta
        } else {
            body.position.x - VELOCITY * delta
        }
        body.setTransform(positionX, body.position.y - VELOCITY * delta, 0f)
        setPosition((body.position.x - width / 2).pixels, (body.position.y - height / 2).pixels)
    }

    companion object {
        private val VELOCITY = 100f.units
    }
}
