package com.yufimtsev.guardian.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.yufimtsev.guardian.utils.units
import ktx.collections.GdxArray

class Duck(textures: List<Texture>, private val deadTexture: Texture, private val startPosition: Vector2, private val flyingRight: Boolean, private val speed: Float) : Sprite() {

    private val animation =
        Animation(0.2f, GdxArray(Array(textures.size) { textures[it] }), PlayMode.LOOP_REVERSED)

    init {
        setBounds(0f, 0f, 32f.units, 32f.units)
        setRegion(textures.first())
    }

    private var timer = 0f
    var currentPositionX = startPosition.x
    var dead = false

    fun update(delta: Float): Boolean {
        timer += delta
        if (!dead) {
            currentPositionX += if (flyingRight) {
                speed
            } else {
                -speed
            }.units * delta
        }
        setPosition(currentPositionX - deadTexture.width.toFloat().units / 2f, startPosition.y)

        if (dead) {
            setRegion(deadTexture)
        } else {
            setRegion(animation.getKeyFrame(timer))
        }
        setFlip(!flyingRight, false)
        return true
    }

}
