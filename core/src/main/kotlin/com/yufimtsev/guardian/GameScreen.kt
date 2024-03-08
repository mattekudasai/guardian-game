package com.yufimtsev.guardian

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.yufimtsev.guardian.GuardianGame.Companion.VIRTUAL_HEIGHT
import com.yufimtsev.guardian.GuardianGame.Companion.VIRTUAL_WIDTH
import com.yufimtsev.guardian.checks.PrecisionCheck
import com.yufimtsev.guardian.disposing.Disposing
import com.yufimtsev.guardian.disposing.Self
import com.yufimtsev.guardian.player.Hud
import com.yufimtsev.guardian.player.Player
import com.yufimtsev.guardian.utils.TextDrawer
import com.yufimtsev.guardian.utils.units
import com.yufimtsev.guardian.world.LayeredMapRenderer
import com.yufimtsev.guardian.world.addBlock
import com.yufimtsev.guardian.world.forEachRectangle
import ktx.app.KtxInputAdapter
import ktx.app.KtxScreen
import ktx.graphics.use
import ktx.tiled.height
import ktx.tiled.layer
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameScreen(
    private val refreshRate: Int,
    private val toggleFullScreen: () -> Unit,
    private val enableDebugRender: Boolean = false
) : KtxScreen, KtxInputAdapter,
    Disposing by Self() {

    private val batch: SpriteBatch by remember { SpriteBatch() }
    private val shapeRenderer: ShapeRenderer by remember {
        ShapeRenderer().apply {
            setColor(0.5f, 0.5f, 0.6f, 1f)
        }
    }
    private var inputProcessorCleared = false

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(
        VIRTUAL_WIDTH.units,
        VIRTUAL_HEIGHT.units,
        camera
    )

    private val mapLoader: TmxMapLoader = TmxMapLoader()
    private val map: TiledMap by remember { mapLoader.load("map/worldMap.tmx") }
    private val mapRenderer: LayeredMapRenderer by remember { LayeredMapRenderer(map) }

    private val world: World by remember { World(Vector2(0f, -5f), true) }
    private val box2dRenderer: Box2DDebugRenderer by remember { Box2DDebugRenderer() }

    private val playerTexture: Texture by remember { Texture("character.png") }
    private val player =
        Player(world, playerTexture, spawnPosition = map.layer("player-spawn").objects.get(0).let {
            Vector2(it.x + it.width / 2f, it.y + it.height / 2f)
        })

    private var virtualPixelSize: Int = 0
    private var virtualScreenOffsetX: Int = 0
    private var virtualScreenOffsetY: Int = 0
    private var screenWidth: Int = 0

    private val hud: Hud by remember {
        Hud(
            { screenWidth },
            { virtualScreenOffsetX },
            { virtualScreenOffsetY },
            { virtualPixelSize })
    }

    private val lettuceTexture: Texture by remember { Texture("lettuce.png") }
    private val logTexture: Texture by remember { Texture("log.png") }
    private val potatoTexture: Texture by remember { Texture("potato.png") }
    private val potatoSaladTexture: Texture by remember { Texture("potato_salad.png") }
    private val nothingTexture: Texture by remember { Texture("nothing.png") }
    private val precisionCheck: PrecisionCheck by remember {
        PrecisionCheck(
            { virtualPixelSize },
            { virtualScreenOffsetY },
            { screenWidth },
            {
                println("difference: $it")
                val threshold = 0.1f
                if (it < threshold) {
                    // do nothing, good enough
                } else {
                    hud.decreaseStamina((it - threshold) / 5f)
                }
            }
        )
    }

    private val characterTextDrawer: TextDrawer by remember { TextDrawer() }

    private val chompSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("chomp.wav")) }
    private val crashSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("crash.wav")) }

    private val finalPhrases = listOf(
        "NICE DRAGON STEW",
        "BUT THE PLACE IS A MESS",
        "TOMORROW WILL BE A BUSY DAY\nCLEANING UP ALL OF THIS",
        "NICE STARRY NIGHT",
        "IT'S CALM, FINALLY",
        "NICE",
        "WHY DO I HAVE A HOUSE\nMADE OF WOODEN BRICKS",
    )

    private var doorStartX: Float = 0f
    private var doorEndX: Float = 0f

    init {
        map.forEachRectangle("block", world::addBlock)
        // TODO: correct this
        map.forEachRectangle("water", world::addBlock)
        map.forEachRectangle("door") {
            doorStartX = it.x.units
            doorEndX = (it.x + it.width).units
        }
        camera.position.y = player.body.position.y + 48f.units
    }

    override fun show() {
        Gdx.input.inputProcessor = this
    }

    private fun showText(text: String) {
        textToShow = text.split("\n")
        characterTextCountdown = 5f
    }

    private fun showLogPowerCheck(appearing: Boolean = true, powerForFixedPosition: Float, count: Int = 3) {
        precisionCheck.show(
            logTexture,
            target = 0.9f,
            speed = 6f,
            disappearIn = 1f,
            isVertical = true,
            powerForFixedPosition = powerForFixedPosition,
            showGuide = false,
            appearing = appearing,
            onActionCallback = { position, delta ->
                if (delta > 0.5f) {
                    chompSound.play(0.6f)
                } else {
                    crashSound.play()
                }
            }) { position, delta ->
            if (delta > 0.5f) {
                showLogPowerCheck(appearing = false, powerForFixedPosition = powerForFixedPosition, count = count)
            } else if (count > 1) {
                showLogPrecisionCheck(appearing = false, count = count - 1)
            } else {
                showText("NICE FIREWOOD")
            }
        }
    }

    private fun showLogPrecisionCheck(appearing: Boolean = true, count: Int = 3) {
        precisionCheck.show(
            logTexture,
            target = 0f,
            speed = 3f,
            disappearIn = 0f,
            isVertical = true,
            powerForFixedPosition = null,
            showGuide = true,
            appearing = appearing
        ) { position, delta ->
            showLogPowerCheck(appearing = false, powerForFixedPosition = position, count = count)
        }
    }

    private fun showLettuceCheck(appearing: Boolean = true, count: Int = 3) {
        precisionCheck.show(
            lettuceTexture,
            target = -0.8f,
            speed = 6f,
            isVertical = false,
            powerForFixedPosition = null,
            showGuide = true,
            appearing = appearing
        ) { position, delta ->
            if (count > 1) {
                showLettuceCheck(appearing = false, count = count - 1)
            } else {
                showText("NICE LETTUCE")
            }
        }
    }

    private fun showPotatoPowerCheck(appearing: Boolean = true, count: Int = 3) {
        precisionCheck.show(
            potatoTexture,
            target = 0.9f,
            speed = 3f,
            disappearIn = 1f,
            isVertical = true,
            powerForFixedPosition = -10f,
            showGuide = false,
            appearing = appearing,
        ) { position, delta ->
            if (count > 1) {
                showPotatoPowerCheck(appearing = false, count = count - 1)
            } else {
                showText("NICE POTATO")
            }
        }
    }

    private fun showDuckHunt() {
        precisionCheck.show(
            null,
            target = 0.6f,
            speed = 5f,
            disappearIn = 0f,
            isVertical = true,
            showGuide = true,
            appearing = false,
            isFullscreen = true,
        ) { position, delta ->
            showText("NICE DUCK")
        }
    }

    private fun showCookingPotatoSalad(appearing: Boolean = true, count: Int = 8) {
        precisionCheck.show(
            potatoSaladTexture,
            target = Random.nextFloat() * 2f - 1f,
            speed = 6f,
            isVertical = count % 2 > 0,
            showGuide = true,
            appearing = appearing,
            disappearIn = if (count == 0) 1f else 0f
        ) { position, delta ->
            if (count > 0) {
                showCookingPotatoSalad(appearing = false, count = count - 1)
            } else {
                showText("NICE POTATO SALAD")
            }
        }
    }

    private var characterTextCountdown: Float = 0f
    private var textToShow: List<String> = listOf("")
    private var indoorFog = 0f
    private var outdoorFog = 1f

    override fun render(delta: Float) {
        // send update signals
        world.step(1f / refreshRate, 6, 2)
        player.update(delta)
        hud.update(delta)

        // clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // update camera position for map rendering
        camera.position.x = player.body.position.x
        val possibleCameraPositionY = player.body.position.y + 48f.units
        // don't follow player when jumping, but follow when falling below the surface
        camera.position.y = min(camera.position.y, possibleCameraPositionY)
        if (player.body.position.y > camera.position.y + 128f.units) {
            //camera.position.y += player.body.position.y -
        }

        // render everything
        viewport.apply() // other viewports at check renderers are applying their own stuff
        // skybox
        shapeRenderer.use(ShapeRenderer.ShapeType.Filled, camera) {
            it.rect(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT)
        }
        if (player.body.position.x < doorEndX) {
            if (indoorFog > 0f) {
                indoorFog = max(0f, indoorFog - delta * 5f)
            }
        } else {
            if (indoorFog < 1f) {
                indoorFog = min(1f, indoorFog + delta * 5f)
            }
        }
        if (player.body.position.x > doorStartX) {
            if (outdoorFog > 0f) {
                outdoorFog = max(0f, outdoorFog - delta * 5f)
            }
        } else {
            if (outdoorFog < 1f) {
                outdoorFog = min(1f, outdoorFog + delta * 5f)
            }
        }
        mapRenderer.renderBackground(camera, indoorFog, outdoorFog)
        batch.use(camera) { player.draw(it) }
        mapRenderer.renderForeground(camera, indoorFog, outdoorFog)
        if (enableDebugRender) {
            box2dRenderer.render(world, camera.combined)
        }

        if (characterTextCountdown > 0f) {
            characterTextCountdown -= delta
            characterTextDrawer.draw(
                batch,
                camera,
                textToShow,
                player.body.position.x,
                player.body.position.y
            )
            if (characterTextCountdown <= 0f) {
                characterTextCountdown = 0f
                characterTextDrawer.clear()
            }
        }

        precisionCheck.updateAndRender(delta)
        hud.render()
    }

    override fun keyDown(keycode: Int): Boolean {
        // TODO: send signals to player
        when (keycode) {
            Keys.F -> toggleFullScreen()
            Keys.Z -> showLettuceCheck()
            Keys.X -> showLogPrecisionCheck()
            Keys.H -> showDuckHunt()
            Keys.P -> showPotatoPowerCheck()
            Keys.Q -> showCookingPotatoSalad()
        }
        precisionCheck.processKeyDown(keycode)
        hud.processKeyDown(keycode)
        return true
    }

    override fun pause() {
        if (Gdx.input.inputProcessor === this) {
            inputProcessorCleared = true
            Gdx.input.inputProcessor = null
        }
    }

    override fun resume() {
        if (inputProcessorCleared) {
            inputProcessorCleared = false
            Gdx.input.inputProcessor = this
        }
    }

    override fun resize(width: Int, height: Int) {
        // keep pixels square!
        val fitsByWidth = (width / VIRTUAL_WIDTH).toInt()
        val fitsByHeight = (height / VIRTUAL_HEIGHT).toInt()
        val minFits = min(fitsByWidth, fitsByHeight)
        val viewportScreenWidth = (VIRTUAL_WIDTH * minFits).toInt()
        val viewportScreenHeight = (VIRTUAL_HEIGHT * minFits).toInt()
        viewport.update(
            viewportScreenWidth,
            viewportScreenHeight
        )
        virtualScreenOffsetX = (width - viewportScreenWidth) / 2
        viewport.screenX = virtualScreenOffsetX
        virtualScreenOffsetY = (height - viewportScreenHeight) / 2
        viewport.screenY = virtualScreenOffsetY
        virtualPixelSize = minFits
        screenWidth = width
    }
}
