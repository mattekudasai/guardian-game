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

    private val layersConfiguration = mapOf(
        "background-p3" to (0.7f to Color(0.3f, 0.3f, 0.3f, 1f)),
        "background-p2" to (0.8f to Color(0.6f, 0.6f, 0.6f, 1f)),
        "background-p1" to (0.9f to Color(0.8f, 0.8f, 0.8f, 1f)),
        "background" to (1f to Color(1f, 1f, 1f, 1f)),
        "foreground" to (1f to Color(1f, 1f, 1f, 1f)),
        "foreground-p1" to (1.2f to Color(0.5f, 0.5f, 0f, 1f)),
    )
    private val spriteBatch: SpriteBatch by remember { SpriteBatch() }
    private val mapRenderer: OrthogonalTiledMapRenderer by remember {
        OrthogonalTiledMapRenderer(map, 1f.units, spriteBatch)
    }

    init {
        map.layers.forEach {
            it.parallaxX = 1f // default parallax rendering breaks pixel alignment, so ignore it
            it.isVisible = false // we will only enable single rendering layer per time
        }
    }

    fun renderBackground(camera: OrthographicCamera) {
        render(camera, "background")
    }

    fun renderForeground(camera: OrthographicCamera) {
        render(camera, "foreground")
    }

    private fun render(camera: OrthographicCamera, prefix: String) {
        val previousCameraX = camera.position.x
        // TODO: figure out Y parallax //val previousCameraY = camera.position.y
        layersConfiguration.filter { it.key.startsWith(prefix) }.forEach {
            camera.position.x = (previousCameraX.pixels * it.value.first).pixels // too many pixels, but it aligns them
            //TODO: figure out Y parallax //camera.position.y = (previousCameraY.pixels * it.value.first).pixels
            camera.update()
            spriteBatch.color = it.value.second

            val layerToRender = map.layer(it.key)
            layerToRender.isVisible = true
            mapRenderer.setView(camera)
            mapRenderer.render()
            layerToRender.isVisible = false
        }
        camera.position.x = previousCameraX
        // TODO: figure out Y parallax //camera.position.y = previousCameraY
        camera.update() // TODO: probably it is too expensive? reconsider
    }
}
