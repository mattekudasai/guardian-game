@file:JvmName("Lwjgl3Launcher")

package com.yufimtsev.guardian.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.getDisplayMode
import com.yufimtsev.guardian.GuardianGame

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return

    var isFullscreen = false
    val displayMode = getDisplayMode()
    val displayRefreshRate = displayMode.refreshRate
    val refreshRateLimit = displayRefreshRate * 1.1 // adds 6.75% over limit to reduce jitter every second

    val configuration = Lwjgl3ApplicationConfiguration().apply {
        setTitle("guardian-of-the-mountain-peak")
        if (isFullscreen) {
            setFullscreenMode(displayMode)
        } else {
            setWindowedMode(3000, 2000)
        }
        useVsync(true)
        setForegroundFPS(refreshRateLimit.toInt())
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
    }
    val toggleFullscreen: () -> Unit = {
        println("Toggling fullscreen")
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            Gdx.graphics.setFullscreenMode(displayMode)
        } else {
            Gdx.graphics.setWindowedMode(3000, 3000)
        }
    }
    Lwjgl3Application(GuardianGame(refreshRateLimit.toInt(), toggleFullscreen, true), configuration)
}
