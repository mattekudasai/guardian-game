package com.yufimtsev.guardian.world

import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import com.yufimtsev.guardian.utils.units

fun TiledMap.forEachRectangle(layerName: String, block: (Rectangle) -> Unit) {
    layers.get(layerName).objects.getByType(RectangleMapObject::class.java)
        .asSequence()
        .map { it.rectangle }
        .forEach(block)
}

fun World.addBlock(bounds: Rectangle) = createDefaultCollider(bounds)

private fun World.createDefaultCollider(bounds: Rectangle) =
    createBody(BodyDef().apply {
        type = BodyDef.BodyType.StaticBody
        position.set(
            (bounds.x + bounds.width / 2).units,
            (bounds.y + bounds.height / 2).units
        )
    }).apply {
        createFixture(FixtureDef().apply {
            shape = PolygonShape().apply {
                setAsBox(
                    (bounds.width / 2).units,
                    (bounds.height / 2).units
                )
            }
        })
    }
