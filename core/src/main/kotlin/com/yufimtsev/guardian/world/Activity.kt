package com.yufimtsev.guardian.world

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import kotlin.math.sin

class Activity(private val markerTexture: Texture, private val position: Rectangle, private val onAction: () -> Unit) :
    Disposing by Self() {

    private val positionStartX = position.x.units
    private val positionEndX = (position.x + position.width).units
    private val positionX = (position.x + position.width / 2f - 8f).units
    private val positionY = (position.y + position.height).units

    private var markerTimer = 0f
    private var isActive = true
    fun render(delta: Float, playerX: Float, playerStanding: Boolean, batch: SpriteBatch) {
        val markerYOffset =
            if (playerStanding && positionStartX < playerX && positionEndX > playerX) {
                if (!isActive) {
                    0f
                } else {
                    sin(markerTimer.also { markerTimer += delta } * 8f) * 4f
                }
            } else {
                isActive = true
                0f
            }
        if (isActive) {
            batch.draw(markerTexture, positionX.pixels, (positionY + markerYOffset.units).pixels, 16f.units, 16f.units)
        }
    }

    fun processKeyDown(keycode: Int, playerX: Float, playerStanding: Boolean): Boolean {
        if (isActive && playerStanding && position.x.units < playerX && (position.x + position.width).units > playerX) {
            if (keycode == Keys.SPACE || keycode == Keys.SHIFT_LEFT || keycode == Keys.SHIFT_RIGHT || keycode == Keys.K || keycode == Keys.Z || keycode == Keys.DOWN || keycode == Keys.S) {
                onAction()
                isActive = false
                return true
            }
        }
        return false
    }

}
