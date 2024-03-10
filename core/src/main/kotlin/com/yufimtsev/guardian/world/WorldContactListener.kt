package com.yufimtsev.guardian.world

import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Manifold
import com.yufimtsev.guardian.player.Player
import ktx.math.minus

class WorldContactListener(private val onNightCollision: (fatal: Boolean) -> Unit) : ContactListener {
    override fun beginContact(contact: Contact) {
        val fixtureA = contact.fixtureA
        val fixtureB = contact.fixtureB
        val playerToSecondObject = (fixtureA.userData as? Player)?.let { it to fixtureB.userData} ?: (fixtureB.userData as? Player)?.let { it to fixtureA.userData }
        if (playerToSecondObject != null) {
            val player = playerToSecondObject.first
            val target = playerToSecondObject.second
            when (target) {
                is Night -> {
                    player.body.applyLinearImpulse(player.body.worldCenter.minus(target.body.worldCenter).scl(8f), player.body.worldCenter, true)
                    onNightCollision(false)
                }
                is Night.Fatal -> onNightCollision(true)
            }
        }
    }

    override fun endContact(contact: Contact?) {}

    override fun preSolve(contact: Contact?, oldManifold: Manifold?) {}

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) {}
}
