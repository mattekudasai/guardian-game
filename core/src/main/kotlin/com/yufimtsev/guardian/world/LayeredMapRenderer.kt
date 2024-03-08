package com.yufimtsev.guardian.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import ktx.tiled.layer

class LayeredMapRenderer(private val map: TiledMap): Disposing by Self() {

    private val layerOrder = listOf(
        "background-p3",
        "background-p2",
        "background-p1",
        "background",
        "foreground",
        "foreground-p1-trans",
        "foreground-p1-solid",
        "foreground-indoor-fog",
        "foreground-outdoor-fog",
    )
    private val layerColors = mapOf(
        "background-p3" to Color(0.3f, 0.3f, 0.3f, 1f),
        "background-p2" to Color(0.6f, 0.6f, 0.6f, 1f),
        "background-p1" to Color(0.8f, 0.8f, 0.8f, 1f),
        "background" to Color(1f, 1f, 1f, 1f),
        "foreground" to Color(1f, 1f, 1f, 1f),
        "foreground-p1-trans" to Color(1f, 1f, 1f, 0.5f),
        "foreground-p1-solid" to Color(1f, 1f, 1f, 1f),
        "foreground-indoor-fog" to Color(1f, 1f, 1f, 1f),
        "foreground-outdoor-fog" to Color(1f, 1f, 1f, 1f),
    )
    private val layerParallax = mutableMapOf<String, Pair<Float, Float>>()
    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }
    private val mapRenderer: OrthogonalTiledMapRenderer by remember {
        OrthogonalTiledMapRenderer(map, 1f.units, spriteBatch)
    }

    init {
        map.layers.forEach {
            layerParallax[it.name] = it.parallaxX to it.parallaxY
            // default parallax rendering breaks pixel alignment, so ignore it
            it.parallaxX = 1f
            it.parallaxY = 1f
            // we will only enable single rendering layer per time
            it.isVisible = false
        }
    }

    fun renderBackground(camera: OrthographicCamera, indoorFog: Float, outdoorFog: Float) {
        render(camera, "background", indoorFog, outdoorFog)
    }

    fun renderForeground(camera: OrthographicCamera, indoorFog: Float, outdoorFog: Float) {
        render(camera, "foreground", indoorFog, outdoorFog)
    }

    private fun render(camera: OrthographicCamera, prefix: String, indoorFog: Float, outdoorFog: Float) {
        val previousCameraX = camera.position.x
        val previousCameraY = camera.position.y
        layerOrder.filter { it.startsWith(prefix) }.forEach {
            val parallax = layerParallax[it] ?: throw IllegalStateException()
            camera.position.x = (previousCameraX.pixels * parallax.first).pixels // too many pixels, but it aligns them
            // no idea why, but that 960f makes the trick to align the map with how it was designed in editor
            camera.position.y = ((previousCameraY.pixels - 960f.units) * parallax.second + 960f.units).pixels
            camera.update()
            spriteBatch.color = layerColors[it]
            if (it.endsWith("indoor-fog")) {
                spriteBatch.color.a = indoorFog
            } else if (it.endsWith("outdoor-fog")) {
                spriteBatch.color.a = outdoorFog
            }

            val layerToRender = map.layer(it)
            layerToRender.isVisible = true
            mapRenderer.setView(camera)
            mapRenderer.render()
            layerToRender.isVisible = false
        }
        camera.position.x = previousCameraX
        camera.position.y = previousCameraY
        camera.update() // TODO: probably it is too expensive? reconsider
    }
}
