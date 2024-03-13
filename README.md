## Guardian of the Mountain Peak

Video game made for [Acerola Jam 0](https://itch.io/jam/acerola-jam-0/rate/2559155) in (almost) two weeks.

Based on libGDX game engine that is open-source itself: https://github.com/libgdx/libgdx

All assets (available for everyone under CC-BY-4.0 license) are made from scratch using these open source tools:
* [Inkscape](https://github.com/inkscape/inkscape), [GIMP](https://github.com/GNOME/gimp) for images
* [Audacity](https://github.com/audacity/audacity), [LMMS](https://github.com/LMMS/lmms) for sound effects and music
* [Tiled](https://github.com/mapeditor/tiled) for the world map

## Game loop overview (as initially planned)

* wake up
* feel rested, but hungry (close to 0, but not enough to drain stamina yet)
* go right to the garden
* pull some potatoes (power check)
* cut some lettuce (precision check)
* pick up some chopped wood, no more left
* go left to the stove, cook and eat a breakfast (mechanics???)
* need to prepare more firewood, go right in the garden
* chop some wood into stash (precision + power check)
* wood ends, need to get more, go into the woods through garden and mountain view
* run through rubble and mountain streams (control speed by moving left/right, jump at correct time)
* chop some woods (multiple precision checks in a row)
* fish? how to tie this up later?
* hunt for ducks

Micro-games so far:
* power check / precision check / both
    * chopping wood - need to align to the middle and chop as hard as possible
        * cut branches first with light blows
        *
    * chopping trees - need to align multiple blows as close as possible
    * gather lettuce
    * pull potatoes
* cooking ???
* run and jump
    * QTE dodge
* hit-a-duck
* climb up

## Diary, tomorrow is the last day!!

Day 1 (friday, day-off, full throttle):
* created a dummy world map out of dummy tiles
* created a dummy character with idle state and running animation
* added box2d to the world
* added simple inputs for character
* implemented layered tile map renderer for parallax
* tested out TeaVM packaging to itch.io
* planned some next steps

Days 2, 3, 4 (saturday, sunday and monday, much less time):
* implement precision check
* implement power check
* implement wood cutter
* implement lettuce cutter
* implement simple text thought bubble, add some contextual thoughts ("ok, one more time" when failed power check)


Days 5, 6 (tuesday, wednesday, workdays):
* sounds for wood splitting
* animated waterfall
* tested TeaVM rollout, found a bug with thin lines rendering

Day 7 (thursday, workday):
* simplified and fixed text drawing
* fixed precision check for webgl
* shrunk the world map
* implemented potato puller
* implemented cooking of potato salad
* implemented indoor/outdoor effect
* started implementing the duck hunt
* added GPLv3 license

Day 8 (friday, workday):
* drafted lost woods with indoor effect
* placed activities on the level, although did not code them
* implemented endings framework and game restart
* implemented title screen
* implemented different control schemes

Day 9 (saturday):
* added new textures for trees and leaves
* added some animation texture for the night sequence
* fixed a bug with player box remaining in old place after restart

Day 10 (sunday):
* added some movement and collision check for the night sequence
* used the forest textures
* implemented wood cutting
* implemented activity triggers

Day 11 (monday, day-off):
* finished the final sequence and the main game loop
* implemented duck hunt
* added some limited day, evening and night
* added more resources for everything
* connected all the pieces together
* LOGIC IS FINAL NOW (except for bugfixes)

Day 12 (tuesday, workday):
* added post-game time attack
* added some sounds
* added snow to the mountain peak
* cut endings to just 8
* fixed a problem of player moving away from precision check (by forbidding player to move away)
* kinda fixed extreme FPS problems

Day 13 (wednesday, workday):
* added more sounds
* fixed some hints problem
* tested, found a bug with teavm and looping sounds, fixed
* SUBMISSION

Known issues:
* cooking is super-predictable (should be harder for everything other than potato salad)

Hanging stuff:
* starry night
* music
    * crickets (intro, night)
    * morning outdoor
    * evening outdoor
    * night sequence theme
    * starry night
    * game over
    * game win
* sound effects (mooore!!!)
    * lettuce pick fail
    * potato pull fail

________________________________
Leaving next section as it was generated, since it has some useful instructions.

# guardian-of-the-mountain-peak

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/tommyettinger/gdx-liftoff).

This project was generated with a Kotlin project template that includes Kotlin application launchers and [KTX](https://libktx.github.io/) utilities.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3.
- `teavm`: Experimental web platform using TeaVM and WebGL.

## Gradle

This project uses [Gradle](http://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/lib`.
- `lwjgl3:run`: starts the application.
- `teavm:build`: builds the JavaScript application into the build/dist/webapp folder.
- `teavm:run`: serves the JavaScript application at http://localhost:8080 via a local Jetty server.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
