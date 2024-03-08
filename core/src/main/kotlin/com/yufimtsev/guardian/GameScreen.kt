package com.yufimtsev.guardian

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
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
import com.yufimtsev.guardian.utils.pixels
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

    private var isGameStarted = false
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
    private var player: Player = createPlayer()

    private fun createPlayer(): Player =
        Player(world, playerTexture, spawnPosition = map.layer("player-spawn").objects.get(0).let {
            Vector2(it.x + it.width / 2f, it.y + it.height / 2f)
        }).autoDisposing()

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

    private val whiteTextDrawer: TextDrawer by remember { TextDrawer("font_white.png", Color.BLACK) }
    private val characterTextDrawer: TextDrawer by remember { TextDrawer("font.png", Color.WHITE) }

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

    private var floorY: Float = 0f
    private var doorStartX: Float = 0f
    private var doorEndX: Float = 0f
    private var lostWoodsEntranceX: Float = 0f
    private var lostWoodsStartX: Float = 0f
    private var lostWoodsEndX: Float = 0f
    private var lostWoodsWidth: Float = 0f
    private var enteredLostWoods: Boolean = false
    private var endingResetTimeout = 0f
    private var characterTextCountdown: Float = 0f
    private var textToShow: List<String> = listOf("")
    private var indoorFog = 0f
    private var outdoorFog = 1f
    private var ending: Ending = Ending.NOTHING_SPECIAL
    private var activePrecisionCheckEnding: Ending? = null

    init {
        map.forEachRectangle("block", world::addBlock)
        map.forEachRectangle("door") {
            doorStartX = it.x.units
            doorEndX = (it.x + it.width).units
        }
        map.forEachRectangle("lost-woods-entrance") {
            lostWoodsEntranceX = it.x.units
        }
        map.forEachRectangle("lost-woods-teleport") {
            lostWoodsStartX = it.x.units
            lostWoodsEndX = (it.x + it.width).units
            lostWoodsWidth = lostWoodsEndX - lostWoodsStartX
        }
        map.forEachRectangle("potato-pull") {}
        map.forEachRectangle("lettuce-pick") {}
        map.forEachRectangle("bed") {}
        map.forEachRectangle("cooking") {}
        map.forEachRectangle("wood-split") {}
        map.forEachRectangle("tree") {}
        map.forEachRectangle("duck-hunt") {}
        map.forEachRectangle("falling-rocks") {}
        map.forEachRectangle("night-hunt") {}
        floorY = player.body.position.y
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
        activePrecisionCheckEnding = Ending.WOOD_SPLITTING_WINS
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
                activePrecisionCheckEnding = null
                showText("NICE FIREWOOD")
            }
        }
    }

    private fun showLogPrecisionCheck(appearing: Boolean = true, count: Int = 3) {
        activePrecisionCheckEnding = Ending.WOOD_SPLITTING_WINS
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
        activePrecisionCheckEnding = Ending.LETTUCE_WINS
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
                activePrecisionCheckEnding = null
                showText("NICE LETTUCE")
            }
        }
    }

    private fun showPotatoPowerCheck(appearing: Boolean = true, count: Int = 3) {
        activePrecisionCheckEnding = Ending.POTATO_WINS
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
                activePrecisionCheckEnding = null
                showText("NICE POTATO")
            }
        }
    }

    private fun showDuckHunt() {
        activePrecisionCheckEnding = Ending.DUCK_WINS
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
            activePrecisionCheckEnding = null
            showText("NICE DUCK")
        }
    }

    private fun showCookingPotatoSalad(appearing: Boolean = true, count: Int = 8) {
        activePrecisionCheckEnding = Ending.POTATO_SALAD_WINS
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
                activePrecisionCheckEnding = null
                showText("NICE POTATO SALAD")
            }
        }
    }


    private fun printWhiteCentered(line: String, y: Float) {
        whiteTextDrawer.draw(
            batch,
            camera,
            listOf(line),
            ((VIRTUAL_WIDTH - line.length * 6f) / 2f).units.pixels,
            y.units.pixels,
            ignoreLastPosition = true
        )
    }
    private fun printBlackCentered(line: String, y: Float) {
        characterTextDrawer.draw(
            batch,
            camera,
            listOf(line),
            ((VIRTUAL_WIDTH - line.length * 6f) / 2f).units.pixels,
            y.units.pixels,
            ignoreLastPosition = true
        )
    }

    override fun render(delta: Float) {
        // clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (!isGameStarted) {
            if (endingResetTimeout <= ENDING_RESET_TIMEOUT_SECONDS) {
                endingResetTimeout += delta
            }
            camera.position.x = (VIRTUAL_WIDTH / 2f).units
            camera.position.y = (VIRTUAL_HEIGHT / 2f).units
            viewport.apply() // other viewports at check renderers are applying their own stuff
            printBlackCentered("GUARDIAN OF THE MOUNTAIN PEAK", VIRTUAL_HEIGHT / 2f + 80f)
            if (endingResetTimeout >= ENDING_NUMBER_TIMEOUT_SECONDS) {
                printWhiteCentered("  MOVE     JUMP     ACTION", VIRTUAL_HEIGHT / 2f + 32f)
                printWhiteCentered(" ARROWS     X         Z  ", VIRTUAL_HEIGHT / 2f + 16F)
                printWhiteCentered("  A D       J         K  ", VIRTUAL_HEIGHT / 2f)
                printWhiteCentered("            UP      SHIFT", VIRTUAL_HEIGHT / 2f - 16F)
                printWhiteCentered("            W       SPACE", VIRTUAL_HEIGHT / 2f - 32F)
            }
            if (endingResetTimeout >= ENDING_RESET_TIMEOUT_SECONDS) {
                printWhiteCentered("PRESS ANY KEY TO START", VIRTUAL_HEIGHT / 2f - 96f)
            }
            return
        }

        val isPlayerInWater = player.body.position.y < floorY
        // setup an ending
        ending = when {
            enteredLostWoods -> Ending.LOST_IN_WOODS
            isPlayerInWater -> if (floorY - player.body.position.y > 6f.units) Ending.DROWN else Ending.FROZEN_BY_STREAM
            activePrecisionCheckEnding != null -> activePrecisionCheckEnding!!
            else -> Ending.NOTHING_SPECIAL
        }
        hud.update(delta, isPlayerInWater, enteredLostWoods)


        if (hud.stamina == 0f) {
            if (endingResetTimeout <= ENDING_RESET_TIMEOUT_SECONDS) {
                endingResetTimeout += delta
            }
            camera.position.x = (VIRTUAL_WIDTH / 2f).units
            camera.position.y = (VIRTUAL_HEIGHT / 2f).units
            viewport.apply() // other viewports at check renderers are applying their own stuff
            printWhiteCentered(ending.text, VIRTUAL_HEIGHT / 2f + 32f)
            if (endingResetTimeout >= ENDING_NUMBER_TIMEOUT_SECONDS) {
                printWhiteCentered("ENDING ${ending.ordinal + 1} OUT OF ${Ending.entries.size}", VIRTUAL_HEIGHT / 2f - 32f)
            }
            if (endingResetTimeout >= ENDING_RESET_TIMEOUT_SECONDS) {
                printWhiteCentered("PRESS ANY KEY TO START OVER", VIRTUAL_HEIGHT / 2f - 64f)
            }
            return
        }

        // send update signals
        world.step(1f / refreshRate, 6, 2)
        player.update(delta, isPlayerInWater)

        // update camera position for map rendering
        camera.position.x = player.body.position.x
        val possibleCameraPositionY = player.body.position.y + 48f.units
        // don't follow player when jumping, but follow when falling below the surface
        camera.position.y = floorY + 48f.units//min(camera.position.y, possibleCameraPositionY)
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
        if (player.body.position.x > doorStartX && player.body.position.x < lostWoodsEntranceX) {
            if (outdoorFog > 0f) {
                outdoorFog = max(0f, outdoorFog - delta * 5f)
            }
        } else {
            if (outdoorFog < 1f) {
                outdoorFog = min(1f, outdoorFog + delta * 5f)
            }
        }
        if (!enteredLostWoods && player.body.position.x > lostWoodsStartX) {
            enteredLostWoods = true
        }
        if (enteredLostWoods) {
            while (player.body.position.x < lostWoodsStartX) {
                player.body.setTransform(player.body.position.x + lostWoodsWidth, player.body.position.y, 0f)
            }
            while (player.body.position.x > lostWoodsEndX) {
                player.body.setTransform(player.body.position.x - lostWoodsWidth, player.body.position.y, 0f)
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
        if (!isGameStarted) {
            if (endingResetTimeout > ENDING_RESET_TIMEOUT_SECONDS) {
                endingResetTimeout = 0f
                isGameStarted = true
            }
            return true
        }
        if (hud.stamina == 0f || isGameStarted) {
            if (endingResetTimeout > ENDING_RESET_TIMEOUT_SECONDS) {
                restart()
            }
            return true
        }
        if (precisionCheck.processKeyDown(keycode)) return true
        when (keycode) {
            Keys.F -> toggleFullScreen()
            Keys.NUM_1 -> showLettuceCheck()
            Keys.NUM_2 -> showLogPrecisionCheck()
            Keys.NUM_3 -> showDuckHunt()
            Keys.NUM_4 -> showPotatoPowerCheck()
            Keys.NUM_5 -> showCookingPotatoSalad()
        }
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


    private fun restart() {
        player = createPlayer()
        hud.reset()
        precisionCheck.reset()
        enteredLostWoods = false
        endingResetTimeout = 0f
        characterTextCountdown = 0f
        textToShow = listOf("")
        indoorFog = 0f
        outdoorFog = 1f
        ending = Ending.NOTHING_SPECIAL
        activePrecisionCheckEnding = null
    }

    enum class Ending(val text: String) {
        NOT_ENDING("GUARDIAN OF THE MOUNTAIN PEAK"),
        DROWN("DROWN IN WATER"),
        FROZEN_BY_STREAM("FROZEN BY MOUNTAIN STREAM"),
        POTATO_WINS("BEATEN BY POTATO"),
        LETTUCE_WINS("BEATEN BY LETTUCE"),
        POTATO_SALAD_WINS("BEATEN BY POTATO SALAD"),
        WOOD_SPLITTING_WINS("COULD NOT SPLIT A LOG"),
        WOOD_CUTTING_WINS("COULD NOT CUT A WOOD"),
        DUCK_WINS("COULD NOT HUNT A DUCK"),
        LOST_IN_WOODS("LOST IN WOODS"),
        NIGHT("DIDN'T SURVIVE THE NIGHT"),
        NOTHING_SPECIAL("STARVED TO DEATH"),
    }

    companion object {
        private val ENDING_RESET_TIMEOUT_SECONDS = 2f
        private val ENDING_NUMBER_TIMEOUT_SECONDS = 1f
    }
}
