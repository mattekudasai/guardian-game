package com.yufimtsev.guardian

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.math.Rectangle
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
import com.yufimtsev.guardian.utils.pixelAligned
import com.yufimtsev.guardian.utils.pixels
import com.yufimtsev.guardian.utils.units
import com.yufimtsev.guardian.world.Activity
import com.yufimtsev.guardian.world.Duck
import com.yufimtsev.guardian.world.Fireball
import com.yufimtsev.guardian.world.LayeredMapRenderer
import com.yufimtsev.guardian.world.Night
import com.yufimtsev.guardian.world.TopWood
import com.yufimtsev.guardian.world.Tree
import com.yufimtsev.guardian.world.WorldContactListener
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

    private var night: Night? = null

    private fun createNight() = Night(world, spawnPosition = map.layer("night-spawn").objects.get(0).let {
        Vector2(it.x + it.width / 2f, it.y + it.height / 2f)
    }, onHeadDown = { night ->
        val headStartX = (night.body.position.x) * GuardianGame.PIXELS_PER_UNIT // backward unit conversion
        val headY = (night.body.position.y) * GuardianGame.PIXELS_PER_UNIT // backward unit conversion

        activities["night-head"] = Activity(markerTexture, Rectangle(headStartX - 104f, headY - 32f, 48f, 16f)) {
            if (night.isFrozen) {
                activities.remove("night-head")
                night.freeze(500f)
                showHeadPrecisionCheck()
            } else {
                ending = Ending.NIGHT
                hud.decreaseStamina(1f)
            }
        }
    }, onHeadUp = {
        activities.remove("night-head")
    }, spawnFireball = { position, goingRight ->
        fireballs.add(
            Fireball(
                world,
                fireballTexture,
                position,
                goingRight
            )
        )
    }).autoDisposing()

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
            { virtualPixelSize },
            { ending = Ending.NOTHING_SPECIAL }
        )
    }

    private val lettuceTexture: Texture by remember { Texture("lettuce.png") }
    private val logTexture: Texture by remember { Texture("log.png") }
    private val potatoTexture: Texture by remember { Texture("potato.png") }
    private val potatoSaladTexture: Texture by remember { Texture("potato_salad.png") }
    private val nothingTexture: Texture by remember { Texture("nothing.png") }
    private val markerTexture: Texture by remember { Texture("marker.png") }
    private val targetTexture: Texture by remember { Texture("target.png") }
    private val fireballTexture: Texture by remember { Texture("fireball.png") }
    private val cookedDuck: Texture by remember { Texture("cooked_duck.png") }
    private val cookedHead: Texture by remember { Texture("cooked_head.png") }
    private val nightSnow: Texture by remember { Texture("night/snow.png") }
    private val precisionCheck: PrecisionCheck by remember {
        PrecisionCheck(
            { virtualPixelSize },
            { virtualScreenOffsetY },
            { screenWidth },
            {
                val threshold = 0.15f
                if (it < threshold) {
                    // do nothing, good enough
                } else if (activePrecisionCheckEnding != Ending.NIGHT && activePrecisionCheckEnding != Ending.NOTHING_SPECIAL) {
                    hud.decreaseStamina((it - threshold) / 40f)
                }
            },
            {
                if (it && night?.isFrozen == true) {
                    night?.freeze(1f)
                }
                activePrecisionCheckEnding = null
            }
        )
    }

    private val whiteTextDrawer: TextDrawer by remember { TextDrawer("font_white.png", Color.BLACK) }
    private val characterTextDrawer: TextDrawer by remember { TextDrawer("font.png", Color.WHITE) }

    private val chompSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("chomp.wav")) }
    private val crashSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("crash.wav")) }
    private val titleTextSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("title_text.wav")) }
    private val deathSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("death.wav")) }
    private val damageSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("damage.wav")) }
    private val speechSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("speech.wav")) }
    private val startSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("start.wav")) }
    private val lettuceSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("lettuce.wav")) }
    private val potatoSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("potato.wav")) }
    private val splashSound = lettuceSound
    private val shotSound = lettuceSound
    private val cookSound = lettuceSound
    private val cookedSound: Sound by remember { Gdx.audio.newSound(Gdx.files.internal("cooked.wav")) }
    private val woodsMusic: Music by remember { Gdx.audio.newMusic(Gdx.files.internal("woods.ogg")) }

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
    private var wasPlayerInWater = false
    private var doorStartX: Float = 0f
    private var doorEndX: Float = 0f
    private var lostWoodsEntranceX: Float = 0f
    private var lostWoodsStartX: Float = 0f
    private var lostWoodsEndX: Float = 0f
    private var lostWoodsWidth: Float = 0f
    private var enteredLostWoods: Boolean = false
    private var endingResetTimeout = 0f
    private var deathSoundDelay = 0f
    private var characterTextCountdown: Float = 0f
    private var textToShow: List<String> = listOf("")
    private var indoorFog = 0f
    private var outdoorFog = 1f
    private var ending: Ending = Ending.NOTHING_SPECIAL
    private var activePrecisionCheckEnding: Ending? = null

    private val activities = mutableMapOf<String, Activity>()
    private var potatoesPulled = 0f
    private var lettucePicked = 0f
    private var woodCollected = 0f
    private var firewoodCollected = 0f
    private var duckCollected = 0f
    private var isGameEnding = false
    private var timeToShowLastCooking = 0f
    private var shouldFade = false
    private var potatoSaladHintShown = false
    private var finishedLateBecauseOfPotatoSalad = 0 // DO NOT RESET ON RESTART

    private val smallTreeTexture: Texture by remember { Texture("small_tree.png") }
    private val mediumTreeTexture: Texture by remember { Texture("medium_tree.png") }
    private val largeTreeTexture: Texture by remember { Texture("large_tree.png") }
    private val forestTopTexture: Texture by remember { Texture("forest_top.png") }
    private val duckTextures = listOf(
        Texture("duck_up.png").autoDisposing(),
        Texture("duck_middle.png").autoDisposing(),
        Texture("duck_down.png").autoDisposing(),
    )
    private val deadDuckTexture: Texture by remember { Texture("duck_dead.png") }
    private val interactiveTrees = mutableMapOf<String, Tree>()
    private val largeTrees = mutableListOf<Tree>()
    private val trees = mutableListOf<Tree>()
    private val topWoods = mutableListOf<TopWood>()
    private val fireballs = mutableListOf<Fireball>()
    private val ducks = mutableListOf<Duck>()

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
        resetActivities()
        map.forEachRectangle("small-tree") {
            trees += Tree(smallTreeTexture, it, scaleFactor = 0.3f)
        }
        map.forEachRectangle("large-tree") {
            largeTrees += Tree(largeTreeTexture, it)
        }
        map.forEachRectangle("medium-tree") {
            trees += Tree(mediumTreeTexture, it, scaleFactor = 0.3f)
        }
        map.forEachRectangle("lost-woods-top") {
            topWoods += TopWood(forestTopTexture, it)
        }
        floorY = player.body.position.y
        camera.position.y = player.body.position.y + 48f.units
        world.setContactListener(WorldContactListener { fatal, fire ->
            if (fatal) {
                ending = Ending.NIGHT
                hud.decreaseStamina(1f)
            } else if (fire) {
                damageSound.play()
                ending = Ending.NIGHT
                hud.decreaseStamina(0.1f)
            } else {
                damageSound.play()
                ending = Ending.NIGHT
                hud.decreaseStamina(0.2f)
            }
        })
    }

    private fun resetActivities() {
        activities.clear()
        interactiveTrees.clear()
        map.forEachRectangle("potato-pull") {
            activities["potato-pull"] =
                Activity(markerTexture, it) { showPotatoPowerCheck(appearing = true, count = 0) }
        }
        map.forEachRectangle("lettuce-pick") {
            activities["lettuce-pick"] = Activity(markerTexture, it) { showLettuceCheck(appearing = true) }
        }
        map.forEachRectangle("bed") {
            activities["bed"] = Activity(markerTexture, it) {
                val night = this.night
                if (night == null) {
                    showText("I DON'T WANT TO SLEEP")
                } else if (night.health == 2.5f) {
                    showText("I NEED TO CHECK\nWHAT'S GOING ON")
                } else if (night.health > 0f) {
                    showText("I NEED TO GET RID\nOF THAT THING")
                } else {
                    ending = Ending.TRUE_ENDING
                    hud.decreaseStamina(1f)
                }
            }
        }
        map.forEachRectangle("cooking") {
            activities["cooking"] = Activity(markerTexture, it) {
                if (potatoesPulled < 3f) {
                    showText("NEED MORE POTATOES")
                } else if (lettucePicked < 3f) {
                    showText("NEED MORE LETTUCE")
                } else {
                    showCookingPotatoSalad(appearing = true, count = 8)
                }
            }
        }
        map.forEachRectangle("wood-split") {
            activities["wood-split"] = Activity(markerTexture, it) {
                if (woodCollected == 0f) {
                    showText("NOT ENOUGH WOOD")
                } else {
                    showLogPrecisionCheck(appearing = true, showGuide = false)
                }
            }
        }
        var index = 0
        map.forEachRectangle("tree") {
            val key = "tree_$index"
            activities[key] = Activity(markerTexture, it) {
                showWoodCutter(key, false)
            }
            interactiveTrees[key] = Tree(smallTreeTexture, it)
            index++
        }

        map.forEachRectangle("duck-hunt") {
            activities["duck-hunt"] = Activity(targetTexture, it) {
                showDuckHunt()
            }
        }
        map.forEachRectangle("falling-rocks") {}
        map.forEachRectangle("stove") {
            activities["stove"] = Activity(markerTexture, it) {
                if (duckCollected == 0f) {
                    showText("NEED SOMETHING TO COOK")
                } else if (firewoodCollected == 0f) {
                    showText("NEED MORE FIREWOOD")
                } else {
                    showCookingDuck(appearing = true, count = 8)
                }
            }
        }
        map.forEachRectangle("night-hunt") {
            activities["night-hunt"] = Activity(markerTexture, it) {
                if (isEvening()) {
                    showText("SNOW HAS ALREADY MELTED")
                } else {
                    showText("LOVE THIS SNOWY\nMOUNTAIN VIEW")
                }
            }
        }
    }

    private fun resetNightHunt() {
        if (night == null) return
        map.forEachRectangle("night-hunt") {
            activities["night-hunt"] = Activity(targetTexture, it) {
                activities.remove("night-hunt")
                showNightHunt(it)
            }
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = this
    }

    private fun showText(text: String) {
        speechSound.play()
        textToShow = text.split("\n")
        characterTextCountdown = 3f
    }

    private var gameTimer = 0f
    private var showGameTimer = false

    private fun showLogPowerCheck(appearing: Boolean = true, powerForFixedPosition: Float, missedCount: Int = 0) {
        activePrecisionCheckEnding = Ending.COULD_NOT_DO
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
                    chompSound.play()
                } else {
                    crashSound.play()
                }
            }) { position, delta ->
            if (delta > 0.5f) {
                showText("HIT HARDER")
                showLogPowerCheck(
                    appearing = false,
                    powerForFixedPosition = powerForFixedPosition,
                    missedCount = missedCount
                )
            } else {
                firewoodCollected += 1f - delta / 2f
                if (firewoodCollected < 3f) {
                    showLogPrecisionCheck(appearing = false, showGuide = missedCount > 1, missedCount = missedCount)
                } else {
                    activities.remove("wood-split")
                    showText("GOT FIREWOOD")
                }
            }
        }
    }

    private fun showLogPrecisionCheck(appearing: Boolean = true, showGuide: Boolean = false, missedCount: Int = 0) {
        activePrecisionCheckEnding = Ending.COULD_NOT_DO
        precisionCheck.show(
            logTexture,
            target = 0f,
            speed = 2f,
            disappearIn = 0f,
            isVertical = true,
            powerForFixedPosition = null,
            showGuide = showGuide,
            appearing = appearing
        ) { position, delta ->
            if (delta > 0.25f) {
                if (missedCount == 1) {
                    showText("AIM FOR THE MIDDLE")
                }
                showLogPrecisionCheck(appearing = false, showGuide = missedCount > 1, missedCount = missedCount + 1)
            } else {
                showLogPowerCheck(appearing = false, powerForFixedPosition = position, missedCount = missedCount)
            }
        }
    }

    private fun showLettuceCheck(appearing: Boolean = true, showGuide: Boolean = false, count: Int = 0) {
        activePrecisionCheckEnding = Ending.COULD_NOT_DO
        precisionCheck.show(
            lettuceTexture,
            target = -0.8f,
            speed = 6f,
            isVertical = false,
            powerForFixedPosition = null,
            showGuide = showGuide,
            appearing = appearing,
            onActionCallback = { _, _ ->
                lettuceSound.play()
            }
        ) { position, delta ->
            lettucePicked += 1f - delta / 2f
            if (lettucePicked >= 3) {
                showText("GOT LETTUCE")
                activities.remove("lettuce-pick")
            } else {
                if (count == 2 && lettucePicked < 2f) {
                    showText("PICK THE MOST")
                }
                showLettuceCheck(appearing = false, showGuide = count >= 3, count = count + 1)
            }
        }
    }

    private fun showPotatoPowerCheck(appearing: Boolean = true, count: Int = 0) {
        activePrecisionCheckEnding = Ending.COULD_NOT_DO
        precisionCheck.show(
            potatoTexture,
            target = 0.9f,
            speed = 3f,
            disappearIn = 1f,
            isVertical = true,
            powerForFixedPosition = -10f,
            showGuide = false,
            appearing = appearing,
            onActionCallback = { _, _ ->
                potatoSound.play()
            }
        ) { position, delta ->
            potatoesPulled += 1f - delta / 2f
            if (potatoesPulled > 3f) {
                showText("GOT POTATO")
                activities.remove("potato-pull")
            } else {
                if (count >= 2 && potatoesPulled < 2f) {
                    showText("PULL HARDER")
                }
                showPotatoPowerCheck(appearing = false, count = count + 1)
            }
        }
    }

    private fun showDuckHunt() {
        activePrecisionCheckEnding = Ending.NOTHING_SPECIAL
        for (i in 0..5) {
            val right = Random.nextBoolean()
            val offsetX = if (right) {
                -VIRTUAL_WIDTH
            } else {
                VIRTUAL_WIDTH
            }.units
            val offsetY = (32f + Random.nextFloat() * 64f).units
            ducks.add(
                Duck(
                    duckTextures,
                    deadDuckTexture,
                    Vector2(player.body.position.x + offsetX, player.body.position.y + offsetY),
                    right,
                    50f + Random.nextFloat() * 100f
                )
            )
        }
        precisionCheck.show(
            null,
            target = 0.6f,
            speed = 2f,
            disappearIn = 1f,
            isVertical = true,
            showGuide = false,
            appearing = false,
            isFullscreen = true,
            onActionCallback = { position, delta ->
                val playerX = player.body.position.x
                val positionOffset = (position * VIRTUAL_WIDTH / 2f).units
                val hitPosition = playerX + positionOffset
                var atLeastOneDead = false
                ducks.forEach {
                    val accuracy = Math.abs(it.currentPositionX - hitPosition)
                    if (accuracy <= 0.2f) {
                        it.dead = true
                        atLeastOneDead = true
                    }
                }
                if (atLeastOneDead) {
                    shotSound.play()
                } else {
                    showText("MISSED")
                }
            }
        ) { position, delta ->
            if (ducks.count { it.dead } > 0) {
                duckCollected = 1f
                showText("NICE DUCK")
                activities.remove("duck-hunt")
            }
            ducks.clear()
            //ducks.filter { it.dead }.forEach { ducks.remove(it) }
        }
    }

    private fun showNightHunt(activity: Activity) {
        activePrecisionCheckEnding = Ending.NIGHT
        precisionCheck.show(
            null,
            target = 0.6f,
            speed = 5f,
            disappearIn = 0f,
            isVertical = true,
            showGuide = false,
            appearing = false,
            isFullscreen = true,
        ) { position, delta ->
            val playerX = player.body.position.x
            night?.let {
                val positionOffset = (position * VIRTUAL_WIDTH / 2f).units
                val hitPosition = playerX + positionOffset

                val accuracy = Math.abs(it.body.position.x - hitPosition)
                if (accuracy < 0.5f) {
                    if (!activities.containsKey("night-head")) {
                        if (it.health > 2f) {
                            showText("CHOOSE A BETTER TIMING")
                        }
                        it.freeze(1f)
                        resetNightHunt()
                    } else {
                        it.freeze(3f)
                    }
                } else {
                    showText("AIM FOR ITS BODY")
                    resetNightHunt()
                }
            }
        }
    }


    private fun showHeadPowerCheck(powerForFixedPosition: Float) {
        precisionCheck.show(
            cookedHead,
            target = 0.9f,
            speed = 6f,
            disappearIn = 1f,
            isVertical = true,
            powerForFixedPosition = powerForFixedPosition,
            showGuide = false,
            appearing = false,
            onActionCallback = { position, delta ->
                night?.freeze(1f)
                chompSound.play()
            }) { position, delta ->
            if (delta > 0.25f) {
                showText("HIT HARDER")
                resetNightHunt()
            } else {
                val health = night?.let {
                    it.health -= 1f - delta * powerForFixedPosition
                    it.health
                } ?: 0f
                if (health > 2f) {
                    showText("IT IS TOUGH")
                } else if (health > 1f) {
                    showText("IT WORKS")
                } else if (health > 0f) {
                    showText("JUST ONE MORE")
                } else {
                    isGameEnding = true
                    night?.freeze(500f)
                    shouldFade = true
                    timeToShowLastCooking = 2f
                    if (!activities.containsKey("cooking") && gameTimer > 80f) {
                        finishedLateBecauseOfPotatoSalad++
                    }
                    activities.keys.filter { !it.equals("bed") }.forEach { activities.remove(it) }
                    printedSounds.clear() // just to make the title screen bump again
                }
                if (!isGameEnding) {
                    resetNightHunt()
                }
            }
        }
    }

    private fun showHeadPrecisionCheck(appearing: Boolean = true, showGuide: Boolean = false, count: Int = 0) {
        activePrecisionCheckEnding = Ending.NIGHT
        precisionCheck.show(
            cookedHead,
            target = 0.5f,
            speed = 3f,
            disappearIn = 0f,
            isVertical = true,
            powerForFixedPosition = null,
            showGuide = showGuide,
            appearing = appearing,
            onActionCallback = { positon, delta ->
                if (count > 3) {
                    night?.freeze(1f)
                }
            }
        ) { position, delta ->
            if (delta > 0.2f) {
                if (count > 2) {
                    night?.freeze(0.01f)
                    showText("LET'S TRY AGAIN")
                    resetNightHunt()
                } else {
                    showText("AIM FOR NECK")
                    showHeadPrecisionCheck(appearing = false, showGuide = count > 1, count + 1)
                }
            } else {
                night?.freeze(500f)
                showHeadPowerCheck(powerForFixedPosition = position)
            }
        }
    }

    private fun showWoodCutter(key: String, showGuide: Boolean = false, missedCount: Int = 0) {
        activePrecisionCheckEnding = Ending.COULD_NOT_DO
        precisionCheck.show(
            null,
            target = -0.45f,
            speed = 3f,
            disappearIn = 1f,
            isVertical = false,
            showGuide = showGuide,
            appearing = false,
            isFullscreen = true,
            onActionCallback = { position, delta ->
                if (delta <= 0.2f) {
                    woodCollected += 1f - delta / 2f
                    if (woodCollected >= 3f) {
                        crashSound.play()
                    } else {
                        chompSound.play()
                    }
                }
            }
        ) { position, delta ->
            if (delta > 0.2f) {
                if (missedCount == 1) {
                    showText("HIT THE TRUNK")
                }
                showWoodCutter(key, showGuide = missedCount > 1, missedCount + 1)
            } else {
                if (woodCollected >= 3f) {
                    showText("GOT SOME WOOD")
                    interactiveTrees.remove(key)
                    activities.remove(key)
                } else {
                    showWoodCutter(key, showGuide = missedCount > 1, missedCount)
                }
            }
        }
    }

    private fun showCookingPotatoSalad(appearing: Boolean = true, count: Int = 7) {
        activePrecisionCheckEnding = Ending.COULD_NOT_COOK
        precisionCheck.show(
            potatoSaladTexture,
            target = -0.6f,
            speed = 5f,
            isVertical = count % 2 > 0,
            showGuide = true,
            appearing = appearing,
            disappearIn = if (count == 0) 1f else 0f,
            onActionCallback = { _, _ ->
                if (count > 0) {
                    cookSound.play()
                } else {
                    cookedSound.play()
                }
            }
        ) { position, delta ->
            if (count > 0) {
                showCookingPotatoSalad(appearing = false, count = count - 1)
            } else {
                showText("NICE POTATO SALAD")
                hud.increaseStamina(1f)
                activities.remove("cooking")
            }
        }
    }

    private fun showCookingDuck(appearing: Boolean = true, count: Int = 7) {
        activePrecisionCheckEnding = Ending.COULD_NOT_COOK
        precisionCheck.show(
            cookedDuck,
            target = -0.6f,
            speed = 5f,
            isVertical = count % 2 > 0,
            showGuide = true,
            appearing = appearing,
            disappearIn = if (count == 0) 1f else 0f,
            onActionCallback = { _, _ ->
                if (count > 0) {
                    cookSound.play()
                } else {
                    cookedSound.play()
                }
            },
        ) { position, delta ->
            if (count > 0) {
                showCookingDuck(appearing = false, count = count - 1)
            } else {
                showText("NICE DUCK")
                hud.increaseStamina(1f)
                activities.remove("stove")
                night = createNight()
                resetNightHunt()
            }
        }
    }

    private fun showCookingNight(appearing: Boolean = true, count: Int = 7) {
        activePrecisionCheckEnding = Ending.TRUE_ENDING
        precisionCheck.show(
            nightSnow,
            target = -0.6f,
            speed = 5f,
            isVertical = count % 2 > 0,
            showGuide = true,
            appearing = appearing,
            disappearIn = if (count == 0) 1f else 0f,
            onActionCallback = { _, _ ->
                if (count > 0) {
                    cookSound.play()
                } else {
                    cookedSound.play()
                }
            },
        ) { position, delta ->
            if (count > 0) {
                showCookingNight(appearing = false, count = count - 1)
            } else {
                night?.let { night ->
                    night.freeze(0.01f)
                    val bodyPosition = night.body.position
                    val x = bodyPosition.x * GuardianGame.PIXELS_PER_UNIT
                    val y = bodyPosition.y * GuardianGame.PIXELS_PER_UNIT
                    val width = 12 * 16f
                    val height = 2 * 16f
                    activities["mess"] =
                        Activity(markerTexture, Rectangle(x - width / 2, y - height / 2, width, height)) {
                            showText("GOTTA CLEAN UP\nTHIS MESS TOMORROW")
                            activities.remove("mess")
                        }
                }
                shouldFade = false
            }
        }
    }

    private fun printWhiteTopLeft(line: String) {
        whiteTextDrawer.draw(
            batch,
            camera,
            listOf(line),
            (camera.position.x - VIRTUAL_WIDTH.units / 2f).pixels,
            (camera.position.y + VIRTUAL_HEIGHT.units / 2f - 10f.units).pixels,
            ignoreLastPosition = true
        )
    }

    private val printedSounds = mutableSetOf<String>()

    private fun printWhiteCentered(line: String, y: Float, silent: Boolean = false) {
        if (!silent && !printedSounds.contains(line)) {
            printedSounds += line
            titleTextSound.play()
        }
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
        if (!printedSounds.contains(line)) {
            printedSounds += line
            titleTextSound.play()
        }
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
            if (endingResetTimeout <= FINAL_ENDING_RESET_TIMEOUT_SECONDS) {
                endingResetTimeout += delta
            }
            camera.position.x = (VIRTUAL_WIDTH / 2f).units
            camera.position.y = (VIRTUAL_HEIGHT / 2f).units
            viewport.apply() // other viewports at check renderers are applying their own stuff
            printBlackCentered("GUARDIAN OF THE MOUNTAIN PEAK", VIRTUAL_HEIGHT / 2f + 96f)
            if (endingResetTimeout >= ENDING_NUMBER_TIMEOUT_SECONDS) {
                printWhiteCentered("BY MATTE FOR ACEROLA JAM 0", VIRTUAL_HEIGHT / 2f + 64f)
            }
            if (endingResetTimeout >= ENDING_RESET_TIMEOUT_SECONDS) {
                printWhiteCentered("  MOVE     JUMP     ACTION", VIRTUAL_HEIGHT / 2f + 32f)
                printWhiteCentered(" ARROWS     X         Z  ", VIRTUAL_HEIGHT / 2f + 16F, silent = true)
                printWhiteCentered("  A D       J         K  ", VIRTUAL_HEIGHT / 2f, silent = true)
                printWhiteCentered("            UP      SHIFT", VIRTUAL_HEIGHT / 2f - 16F, silent = true)
                printWhiteCentered("            W       SPACE", VIRTUAL_HEIGHT / 2f - 32F, silent = true)
            }
            if (endingResetTimeout >= FINAL_ENDING_RESET_TIMEOUT_SECONDS) {
                printWhiteCentered("PRESS ACTION TO START", VIRTUAL_HEIGHT / 2f - 96f)
            }
            return
        }

        val isPlayerInWater = player.body.position.y < floorY
        if (isPlayerInWater != wasPlayerInWater) {
            wasPlayerInWater = isPlayerInWater
            splashSound.play()
        }
        // setup an ending
        ending = when {
            enteredLostWoods -> Ending.LOST_IN_WOODS
            isPlayerInWater -> if (floorY - player.body.position.y > 6f.units) Ending.DROWN else Ending.FROZEN_BY_STREAM
            activePrecisionCheckEnding != null -> activePrecisionCheckEnding!!
            else -> ending//Ending.NOTHING_SPECIAL
        }

        if (hud.stamina == 0f) {
            val deathDelay = 1f
            if (deathSoundDelay == 0f) {
                if (woodsMusic.isPlaying) {
                    woodsMusic.stop()
                }
                deathSound.play()
            }
            if (deathSoundDelay < deathDelay) {
                deathSoundDelay += delta
            }
            if (deathSoundDelay < deathDelay) {
                return
            }
            if (endingResetTimeout <= FINAL_ENDING_RESET_TIMEOUT_SECONDS) {
                endingResetTimeout += delta + (deathSoundDelay - deathDelay)
            }
            deathSoundDelay = deathDelay
            camera.position.x = (VIRTUAL_WIDTH / 2f).units
            camera.position.y = (VIRTUAL_HEIGHT / 2f).units
            viewport.apply() // other viewports at check renderers are applying their own stuff
            printWhiteCentered(ending.text, VIRTUAL_HEIGHT / 2f + 32f)
            if (ending == Ending.TRUE_ENDING) {
                if (endingResetTimeout >= ENDING_NUMBER_TIMEOUT_SECONDS) {
                    printWhiteCentered(
                        "ENDING ${ending.ordinal + 1} OUT OF ${Ending.entries.size}",
                        VIRTUAL_HEIGHT / 2f
                    )
                }
                if (endingResetTimeout >= ENDING_RESET_TIMEOUT_SECONDS) {
                    val formattedTimer = formatGameTimer(gameTimer)
                    val timerText = if (gameTimer > 90f) {
                        "TIME: $formattedTimer OUT OF ${formatGameTimer(90f)}"
                    } else if (gameTimer > 62.578f) {
                        "$formattedTimer VS MATTE'S BEST ${formatGameTimer(62.578f)}"
                    } else {
                        "$formattedTimer, WOW, TELL ME HOW IN COMMENTS"
                    }
                    printWhiteCentered(
                        timerText,
                        VIRTUAL_HEIGHT / 2f - 32f
                    )
                }
                if (endingResetTimeout >= FINAL_ENDING_RESET_TIMEOUT_SECONDS) {
                    printWhiteCentered("PRESS ACTION TO START OVER", VIRTUAL_HEIGHT / 2f - 64f)
                }
            } else {
                if (endingResetTimeout >= ENDING_NUMBER_TIMEOUT_SECONDS) {
                    printWhiteCentered(
                        "ENDING ${ending.ordinal + 1} OUT OF ${Ending.entries.size}",
                        VIRTUAL_HEIGHT / 2f - 32f
                    )
                }
                if (endingResetTimeout >= ENDING_RESET_TIMEOUT_SECONDS) {
                    printWhiteCentered("PRESS ACTION TO START OVER", VIRTUAL_HEIGHT / 2f - 64f)
                }
            }
            return
        }
        if (!isGameEnding) {
            hud.update(delta, isPlayerInWater, enteredLostWoods)
        }

        // send update signals
        world.step(1f / min(60, refreshRate), 6, 2)
        if (!shouldFade) {
            player.update(delta, isPlayerInWater, precisionCheck.showing && !precisionCheck.isFullscreen, refreshRate)
        }
        night?.update(delta, player.body.position.x)
        if (timeToShowLastCooking > 0f) {
            timeToShowLastCooking -= delta
            if (timeToShowLastCooking <= 0f) {
                showCookingNight()
            }
        }

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
            if (night != null) {
                it.color.set(0.1f, 0.1f, 0.15f, 1f)
            } else if (isEvening()) {
                pixelAligned = false
                it.color.set(0.4f, 0.26f, 0.32f, 1f)
            } else {
                it.color.set(0.5f, 0.5f, 0.6f, 1f)
            }
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
        if (isGameEnding && shouldFade) {
            if (outdoorFog < 1f) {
                outdoorFog = min(1f, outdoorFog + delta * 5f)
            }
        } else if (player.body.position.x > doorStartX && player.body.position.x < lostWoodsEntranceX) {
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
            woodsMusic.play()

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
        batch.use(camera) {
            player.draw(it)
            if (night == null && !isEvening()) {
                // parallax snow mountain
                val centerX = 12.351925f
                val x = centerX + (player.body.position.x - centerX) * 0.3f
                val y = floorY + 124f.units
                val width = 20f.units
                if (x < camera.position.x + (VIRTUAL_WIDTH / 2f).units && x > camera.position.x - (VIRTUAL_WIDTH / 2f).units - width) {
                    pixelAligned = false
                    it.draw(
                        nightSnow,
                        x.pixels,
                        y.pixels,
                        width,
                        12f.units,
                        0,
                        0,
                        nightSnow.width,
                        nightSnow.height,
                        true,
                        false
                    )
                } else {
                    pixelAligned = !showGameTimer
                }
            }
            night?.draw(it)
            largeTrees.forEach { it.render(batch) }
            topWoods.forEach { it.render(batch) }
            trees.forEach { it.render(batch) }
            interactiveTrees.values.forEach { it.render(batch) }
            fireballs.asSequence().filter { it.isDestroyed }.toList().forEach {
                fireballs.remove(it)
                world.destroyBody(it.body)
            }
            fireballs.forEach {
                it.updatePosition(delta)
                it.draw(batch)
                if (it.body.position.y < floorY - 32f.units) {
                    it.isDestroyed = true
                }
            }
            ducks.filter { !it.update(delta) }.forEach { ducks.remove(it) }
            ducks.forEach { it.draw(batch) }
            for (activity in activities.values) {
                activity.render(delta, player.body.position.x, player.body.linearVelocity.y == 0f, it)
            }
        }
        mapRenderer.renderForeground(camera, indoorFog, outdoorFog)
        if (enableDebugRender) {
            box2dRenderer.render(world, camera.combined)
        }

        if (characterTextCountdown > 0f) {
            characterTextCountdown -= delta
            val maxLineLength = textToShow.maxOf { it.length }
            characterTextDrawer.draw(
                batch,
                camera,
                textToShow,
                player.body.position.x - (maxLineLength * 3f).units,
                player.body.position.y + 12f.units
            )
            if (characterTextCountdown <= 0f || shouldFade) {
                characterTextCountdown = 0f
                characterTextDrawer.clear()
            }
        }
        if (showGameTimer) {
            printWhiteTopLeft(formatGameTimer(gameTimer))
        }
        if (!potatoSaladHintShown && night == null && finishedLateBecauseOfPotatoSalad > 1) {
            potatoSaladHintShown = true
            showText("DO I EVEN NEED\nPOTATO SALAD")
        }
        gameTimer += delta

        precisionCheck.updateAndRender(delta, isGameEnding)
        if (!isGameEnding || (shouldFade && outdoorFog < 1f)) {
            hud.render()
        }

    }

    private fun formatGameTimer(timer: Float): String {
        val millis = (timer * 1000).toInt() % 1000
        val totalSeconds = timer.toInt()
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return "%02d:%02d.%03d".format(minutes, seconds, millis)
    }

    private fun isEvening() = duckCollected > 0f && (woodCollected > 2.5f || firewoodCollected > 0f)

    override fun keyDown(keycode: Int): Boolean {
        // TODO: send signals to player
        if (!isGameStarted) {
            if (keycode == Input.Keys.SPACE || keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT || keycode == Input.Keys.K || keycode == Input.Keys.Z) {
                if (endingResetTimeout > FINAL_ENDING_RESET_TIMEOUT_SECONDS) {
                    endingResetTimeout = 0f
                    isGameStarted = true
                    printedSounds.clear() // just to make the "press action" bump again
                    startSound.play()
                }
            }
            return true
        }
        if (hud.stamina == 0f && isGameStarted) {
            if (ending == Ending.TRUE_ENDING && endingResetTimeout > FINAL_ENDING_RESET_TIMEOUT_SECONDS || endingResetTimeout > ENDING_RESET_TIMEOUT_SECONDS) {
                if (keycode == Input.Keys.SPACE || keycode == Input.Keys.SHIFT_LEFT || keycode == Input.Keys.SHIFT_RIGHT || keycode == Input.Keys.K || keycode == Input.Keys.Z) {
                    restart()
                }
            }
            return true
        }
        if (precisionCheck.processKeyDown(keycode)) return true
        if (!precisionCheck.showing) {
            for (activity in activities.values) {
                if (activity.processKeyDown(
                        keycode,
                        player.body.position.x,
                        player.body.linearVelocity.y == 0f
                    )
                ) return true
            }
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
        if (showGameTimer || ending == Ending.TRUE_ENDING) {
            showGameTimer = true
        }
        gameTimer = 0f
        night?.let {
            world.destroyBody(it.body)
            night = null
        }
        world.destroyBody(player.body)
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
        woodCollected = 0f
        potatoesPulled = 0f
        lettucePicked = 0f
        resetActivities()
        pixelAligned = !showGameTimer
        isGameEnding = false
        shouldFade = false
        timeToShowLastCooking = 0f
        duckCollected = 0f
        firewoodCollected = 0f
        fireballs.clear()
        ducks.clear()
        potatoSaladHintShown = false
        deathSoundDelay = 0f
        wasPlayerInWater = false
        printedSounds.clear()
        startSound.play()
    }

    enum class Ending(val text: String) {
        TRUE_ENDING("GUARDIAN OF THE MOUNTAIN PEAK"),
        DROWN("DROWN IN WATER"),
        FROZEN_BY_STREAM("FROZEN BY MOUNTAIN STREAM"),
        COULD_NOT_DO("COULDN'T COMPLETE A TASK"),
        COULD_NOT_COOK("STARVED WHILE COOKING"),
        LOST_IN_WOODS("LOST IN WOODS"),
        NIGHT("DIDN'T SURVIVE THE NIGHT"),
        NOTHING_SPECIAL("STARVED TO DEATH"),
    }

    companion object {
        private val FINAL_ENDING_RESET_TIMEOUT_SECONDS = 3f
        private val ENDING_RESET_TIMEOUT_SECONDS = 2f
        private val ENDING_NUMBER_TIMEOUT_SECONDS = 1f
    }
}

