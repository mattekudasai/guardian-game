# Game overview

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

# Diary, 3 more days to go

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

# Plan

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

Day 9:
* added new textures for trees and leaves
* added some animation texture for the night sequence
* fixed a bug with player box remaining in old place after restart

Day 10:
* added some movement and collision check for the night sequence
* used the forest textures
* implemented wood cutting
* implemented activity triggers

Hanging stuff:
* night sequence activities
* fire capture
* simplified duck hunt (precise check simulating light gun)
* world backgrounds
    * morning / day
    * evening
* starry night

Day 11 plan (monday):
* music
  * crickets (intro, night)
  * morning outdoor
  * lost woods theme (owls)
  * evening outdoor
  * night sequence theme
  * starry night
* sound effects (mooore!!!)
  * lettuce pick
  * potato pull
  * salad chop
  * wood cut
  * arrow launch
  * arrow hit
  * lost woods sounds (howling?)
  * night sequence sounds (quite a few)
  * footsteps / falling on rock, soil, grass
  * water / footsteps in water
  * roasting a duck
  * cooking a late dinner

Day 12 (tuesday, workday):
* testing / fixing

Day 13 (wednesday, workday):
* testing / fixing
* submit!!!

Loose ends:
* implement first run-and-jump sequence
* implement second run-and-jump sequence with dodging
* implement woodcutting
* implement wall climbing (how??)
* chicken
