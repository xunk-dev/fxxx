# Black & White theme: Mono Glow identity

Problem: the `Black` accent is flagged `neutral`, and every glow helper returned an empty
Modifier for neutral, so the theme had no light identity at all (flat grey).

Concept: one light source, two behaviours.
- Dark  = EMISSION. White bloom (quadratic falloff) + lit top rim + top spotlight.
- Light = INK. White can't glow on white, so glow becomes a soft downward contact shadow,
          ink-black buttons, bottom paper-grey pooling on the white canvas.

Files
- glass/Glow.kt                 monoGlow(): white bloom (dark) / ink shadow (light). Caller colour ignored.
- theme/Gradients.kt            frameMonoAmbient() (spotlight / paper pools), lit card top, rim-light card edge,
                                titanium (dark) / ink (light) Brand + Fab ramps.
- theme/FrameControlStyle.kt    selected + solid edges tuned for mono.
- theme/Color.kt                dark neutral surfaces only (see v2). Light palette is unchanged.
- theme/Accent.kt               Black swatch preview white -> silver -> black.
- glass/GlassSurface.kt         glowing rim uses rim tone instead of caller colour in mono.
- dock/LiquidDock.kt            bubble glow enabled for mono (white / ink).
- app/home/HomeAmbient.kt       uses frameMonoAmbient for neutral.

Not compiled here (no Gradle/network in the sandbox): please run a build.

## v2: Obsidian (black on black)
Dark neutral no longer uses mid-grey fills. Surfaces: #080808 / #0E0E0E / #141414 / #1E1E1E.
Buttons / FAB / dock bubble / Pro banner = deep black glass (#3A3A3A->#1C1C1C, #2E2E2E->#0B0B0B),
separated only by a lit white rim (0.45 to 0.60 alpha) and the white emission glow.
Files: Color.kt, Gradients.kt, FrameFab.kt, FrameChargeFab.kt, LiquidDock.kt, HomeLook.kt.

## v3: verification pass
Found and fixed by auditing existing unit tests + computing contrast:
- BlackThemeContrastTest.darkModeIsGraphiteNotPureBlack expected #121212 -> updated to #080808.
- darkBrandAndFabFills test used the old grey stops -> rewritten for obsidian fills + rim.
- Light bg #F4F4F4 dropped the Pro-gold badge contrast to 1.14 (< 1.2, existing test) -> light palette reverted.
- Obsidian Brand fill vs switch OFF track = 1.02 (ON/OFF indistinguishable) -> new FrameGradients.Active
  (lit silver #F2F2F2->#BDBDBD) + frameOnActive() ink, applied to Switch, Segmented control, Radio, Theme tile badge.
- Added test activeStateFillIsDistinctFromTheOffTrackAndCarriesInk.
Still NOT verified: Gradle build / unit tests could not be run in the sandbox. Run: ./gradlew :app:testDebugUnitTest
