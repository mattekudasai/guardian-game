package com.yufimtsev.guardian.world

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.yufimtsev.guardian.utils.units

class Tree(private val texture: Texture, private val position: Rectangle, private val scaleFactor: Float = 0.2f) {

    private val positionX = (position.x + position.width / 2f - texture.width*scaleFactor/2f).units
    private val positionY = (position.y - 10f).units

    fun render(batch: SpriteBatch) {
        batch.draw(texture, positionX, positionY, (texture.width.toFloat() * scaleFactor).units, (texture.height.toFloat() * scaleFactor).units)
    }

}
