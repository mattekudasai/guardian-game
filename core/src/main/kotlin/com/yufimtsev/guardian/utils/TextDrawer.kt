package com.yufimtsev.guardian.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import ktx.graphics.use

class TextDrawer(fontFileName: String, private val backgroundColor: Color) : Disposing by Self() {

    private val font: Texture by remember { Texture(fontFileName) }
    private val letters = mutableMapOf<Char, TextureRegion>().apply {
        var index = 0
        fun putRegion(key: Char) = put(key, TextureRegion(font, (index++) * 6, 0, 7, 9))
        ('A'..'Z').forEach(::putRegion)
        putRegion('.')
        putRegion(',')
        putRegion('\'')
        ('0'..'9').forEach(::putRegion)
        putRegion(':')
    }
    private val shapeRenderer: ShapeRenderer by remember { ShapeRenderer() }

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastText: List<String>? = null

    fun clear() {
        lastX = 0f
        lastY = 0f
    }

    fun draw(
        batch: SpriteBatch,
        camera: OrthographicCamera,
        text: List<String>,
        x: Float,
        y: Float,
        ignoreLastPosition: Boolean = false
    ) {
        if (ignoreLastPosition || lastX == 0f && lastY == 0f || !text.equals(lastText)) {
            lastX = x
            lastY = y
            lastText = text
        }
        val maxTextLength = text.maxOf { it.length }
        shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.color = backgroundColor
            it.rect(
                lastX.pixels,
                (lastY + 1f.units).pixels,
                (maxTextLength * 6f + 1).units.pixels,
                (text.size * 9f + 1).units.pixels
            )
        }
        var currentPositionY = lastY + ((text.size - 1) * 9f).units
        batch.use(camera) {
            text.forEach { string ->
                var currentPositionX = lastX
                string.forEach {
                    if (it != ' ') {
                        batch.draw(
                            letters[it],
                            currentPositionX.pixels,
                            currentPositionY.pixels,
                            6f.units.pixels,
                            9f.units.pixels
                        )
                    }
                    currentPositionX += 6f.units
                }
                currentPositionY -= 9f.units
            }
        }
    }
}
