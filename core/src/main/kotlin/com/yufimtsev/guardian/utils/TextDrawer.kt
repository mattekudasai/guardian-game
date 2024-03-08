package com.yufimtsev.guardian.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import ktx.graphics.use

class TextDrawer : Disposing by Self() {

    private val font: Texture by remember { Texture("font.png") }
    private val shapeRenderer: ShapeRenderer by remember { ShapeRenderer() }

    private var lastX: Float = 0f
    private var lastY: Float = 0f

    fun clear() {
        lastX = 0f
        lastY = 0f
    }

    fun draw(batch: SpriteBatch, camera: OrthographicCamera, text: List<String>, x: Float, y: Float) {
        if (lastX == 0f && lastY == 0f) {
            lastX = x
            lastY = y
        }
        val maxTextLength = text.maxOf { it.length }
        shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.color = Color.WHITE
            it.rect(lastX.pixels, (lastY + 1f.units).pixels, (maxTextLength * 6f + 1).units.pixels, (text.size * 9f + 1).units.pixels)
        }
        var currentPositionY = lastY + ((text.size - 1) * 9f).units
        batch.use(camera) {
            text.forEach { string ->
                var currentPositionX = lastX
                string.forEach {
                    if (it != ' ') {
                        val charX = when (it) {
                            in 'A'..'Z' -> it - 'A'
                            '.' -> 26
                            ',' -> 27
                            '\'' -> 28
                            else -> throw IllegalArgumentException()
                        } * 6
                        //batch.draw(font, currentPosition, y, 5f.units, 7f.units, 0, 0, 6, 7, false, false)
                        batch.draw(
                            font,
                            currentPositionX.pixels,
                            currentPositionY.pixels,
                            6f.units,
                            9f.units,
                            charX,
                            0,
                            6,
                            9,
                            false,
                            false
                        )
                        //batch.draw(font, currentPosition, y, 5f, 7f, 5, 7, 5, 7, false, false)
                    }
                    currentPositionX += 6f.units
                }
                currentPositionY -= 9f.units
            }
        }
    }
}
