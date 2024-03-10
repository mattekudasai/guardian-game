package com.yufimtsev.guardian.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.yufimtsev.guardian.utils.units

class TopWood(private val texture: Texture, private val position: Rectangle) {

    private val positionStartX = (position.x/* - position.width / 2f*/).units
    private val width = position.width.units
    private val height = position.height.units
    private val positionY = position.y.units

    fun render(batch: SpriteBatch) {
        batch.draw(texture, positionStartX, positionY, width, height, 0, 0, texture.width, texture.height, false, false)
    }

}
