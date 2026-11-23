# Onboarding clip

The first screen ("Capture the decisive moment") and the last screen ("Ready") share ONE clip:
`app/src/main/assets/onboarding/reel.mp4`

- 640x640 (1:1), 24 fps, ~20.5 s, no audio, HEVC (hvc1), ~1.4 MB, keyframe every 12 frames so frame grabs and loop seeks stay instant.
- 10 graded shots (5 sources: motocross, eagle, snowboard, cars, jet), 5 in the first round, then 5 more.
  Transitions: 0.2 s white flash / slides. The end of the loop (jet) leads into the start (jet).
- Screen 1 plays it from 0:00 and builds the filmstrip from real frames of it.
- Screen 3 (Ready) plays the same file starting at 11.0 s (second round); the capture flashes and
  side frames follow the shot mid-points listed in `OnboardingReadyLive.kt` (CAPTURES).
- If you re-cut the reel, update `FILM_FRACTIONS` (OnboardingHero.kt), `CAPTURES` (OnboardingReadyLive.kt),
  `ReadyAt` (OnboardingReady.kt) and `READY_START_MS` (OnboardingClip.kt).

Re-encode example:

    ffmpeg -i master.mp4 -an -c:v libx265 -preset slow -crf 29 -tag:v hvc1 -x265-params keyint=12:min-keyint=12:open-gop=0:scenecut=0 -pix_fmt yuv420p -movflags +faststart reel.mp4
