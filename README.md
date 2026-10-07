# Diegetic Soup

Diegetic (in-world) UIs for [Minestom](https://minestom.net) and [Paper](https://papermc.io) Minecraft servers.
UIs are built from text and item display entities that are sent as packets only, so they cost the server
no real entities, and the library only sends packets when something a player can see actually changes.

- **Java builders and a Kotlin DSL** for the same API
- **Flexbox layout**: text, items and containers laid out like CSS flexbox, with wrapping text and backgrounds
- **Controllers** that decide who sees a UI and where it is: fixed, following a player, riding an entity
- **Efficient updates**: per-entity diffing, so a static UI sends nothing after it spawns, and smooth
  client-side interpolation for animations and movement
- Tested against a real Minecraft client with [NebsClient](https://github.com/DaylightNebula/NebsClient)

> **Status: 0.1.0, experimental.** The API may change between minor versions. Minestom is tested end to
> end; Paper compiles and shares all of the core, but hasn't been tested on a live server yet.

| Module | Artifact | What it is |
|--------|----------|------------|
| [`core`](core) | `diegetic-core` | Controllers, elements, the Java builders and Kotlin DSL, and the flexbox layout. Platform independent. |
| [`minestom`](minestom) | `diegetic-minestom` | Minestom support (packets, players, locations). Includes `core`. |
| [`paper`](paper) | `diegetic-paper` | Paper support through [PacketEvents](https://github.com/retrooper/packetevents). Includes `core`. |

Requirements: Java 25 and Minecraft 26.x. Paper servers also need the PacketEvents plugin.

## Installation

Releases are served by [JitPack](https://jitpack.io/#DaylightNebula/DiegeticSoup):

```kotlin
// settings.gradle.kts or build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

// build.gradle.kts
dependencies {
    implementation("com.github.DaylightNebula.DiegeticSoup:diegetic-minestom:v0.1.0")
    // or, for a Paper plugin (shade it into your plugin jar):
    implementation("com.github.DaylightNebula.DiegeticSoup:diegetic-paper:v0.1.0")
}
```

Jars are also attached to each [GitHub release](https://github.com/DaylightNebula/DiegeticSoup/releases).

## Setup

Register the platform once when the server starts.

**Minestom**

```kotlin
DiegeticAPI.set(MinestomDiegeticAPI().apply(MinestomDiegeticAPI::init))
```

**Paper** (in your plugin's `onEnable`, after PacketEvents is initialised)

```kotlin
val api = PaperDiegeticAPI()
api.init(this)
DiegeticAPI.set(api)
```

## Concepts

A UI is a **controller** made of three parts:

- a **viewer controller** that decides who sees it: `StaticViewerController` (fixed players),
  `InRadiusViewerController`, `SinglePlayerNearbyViewerController`, `RemoveEmptyViewerController`
  (removes the UI once nobody is watching) or `DynamicViewerController` (your own callback)
- a **position controller** that decides where it is: `StaticPositionController`,
  `PlayerPositionController` (in front of a player) or `DynamicPositionController`
- an **element** that decides what is drawn: a `DSLElement` tree of items and text, a `FlexElement`
  layout, or your own `DiegeticElement`

Every tick the controller renders the element and compares it with what each viewer last received, then
sends only the spawns, updates, moves and removals that are needed.

## Java

```java
DSLController.create()
    .viewerController(new StaticViewerController(List.of(new MinestomPlayer(player))))
    .positionController(new StaticPositionController(new MinestomLocation(pos)))
    .element(DSLElement.create()
        .scale(() -> new Vector3f(0.5f + 0.1f * (float) Math.sin(System.currentTimeMillis() / 300.0)))
        .draw(DSLRenderedElement.create()
            .item(new MinestomItem(ItemStack.of(Material.DIAMOND))))
        .draw(DSLRenderedElement.create()
            .text(Component.text("Hello world!"))
            .translation(new Vector3f(0f, 0.6f, 0f))))
    .spawn();
```

`spawn()` builds the controller and shows it; `build()` builds it without showing it.

## Kotlin

```kotlin
diegetic {
    viewers = StaticViewerController(listOf(MinestomPlayer(player)))
    position = StaticPositionController(MinestomLocation(pos))

    element {
        scale { Vector3f(0.5f + 0.1f * sin(System.currentTimeMillis() / 300.0).toFloat()) }
        draw { item = MinestomItem(ItemStack.of(Material.DIAMOND)) }
        draw {
            text = Component.text("Hello world!")
            translation = Vector3f(0f, 0.6f, 0f)
        }
    }
}
```

The DSL is a thin layer of extension functions over the Java builders; import `dsh.diegetic.controller.*`,
`dsh.diegetic.elements.*` and `dsh.diegetic.flex.*` to use it. `diegetic(autoSpawn = false) { }` builds
without showing.

## Flexbox layouts

`FlexElement` lays out **containers**, **text** and **items** the way CSS flexbox does: direction, wrap,
`justify-content`, `align-items`/`align-self`/`align-content`, gap, padding, margin, grow/shrink/basis and
width/height/min/max in pixels or percentages. Text is measured with Minecraft's default font and wraps
to its box, exactly where the client wraps it. Containers can have a background colour.

Sizes are in text pixels: 1 px is 1/40 of a block at scale 1.

```kotlin
diegetic {
    viewers = StaticViewerController(listOf(MinestomPlayer(player)))
    position = StaticPositionController(MinestomLocation(pos))

    flex(scale = 0.5f) {
        direction(FlexDirection.COLUMN); width(200); padding(8); gap(6); background(0xE0101820)

        row {
            justifyContent(JustifyContent.SPACE_BETWEEN)
            text(Component.text("Shop", NamedTextColor.GOLD))
            text(Component.text("3 items", NamedTextColor.GRAY))
        }
        text("Pick an item below. Long text wraps to fit the panel.")
        row {
            gap(6)
            column {
                alignItems(AlignItems.CENTER); padding(4); grow(1f); background(0x30FFFFFF)
                item(MinestomItem(ItemStack.of(Material.DIAMOND))) { size(24, 24) }
                text("Diamond")
            }
        }
    }
}
```

The same in Java:

```java
FlexElement.create(
    FlexContainer.column().width(200).padding(8).gap(6).background(0xE0101820)
        .child(FlexText.create("Pick an item below."))
        .child(FlexItem.create(new MinestomItem(ItemStack.of(Material.DIAMOND))).size(24, 24))
).scale(0.5f).anchor(Anchor.CENTER);
```

The panel faces +z and is centred on the element's origin by default (`anchor`). Layout runs every tick,
so changing a node's text or style reflows the panel, and only the entities that move or change are
updated. Not supported yet: `margin: auto`, `order`, baseline alignment, `aspect-ratio` and percentage
padding.

## Interpolation

Controllers interpolate on the client by default, so animations stay smooth between server ticks:

- `interpolationDuration(ticks)`: how long the client blends an element to a new transform (default 1;
  0 snaps)
- `teleportDuration(ticks)`: how long the client takes to move a UI to a new position (0 to 59, default 1)

## Building and testing

```bash
./gradlew build
```

The scenario suite in [`test-minestom`](test-minestom) starts a Minestom server and drives a real Minecraft
client with NebsClient. It checks the builders and DSL, the packets actually sent, the flexbox layout
against reference layouts from a browser, and text, item and background geometry against the client's
screenshots.

```bash
./gradlew :test-minestom:runMinestomTests                      # everything (launches a client)
./gradlew :test-minestom:runMinestomTests -PskipClient=true    # only the scenarios that need no client
./gradlew :test-minestom:runMinestomTests -Ponly=flex-css-justify-center,remove-controller-despawns
./gradlew :test-minestom:runMinestomTests -PkeepClient=true    # leave the client running for the next run
```

The first client launch downloads Minecraft (about 700 MB) into `.nebs/`. The client needs a display; on
headless Linux, run the suite under `xvfb-run`. Screenshots are saved to `test-minestom/build/test-screenshots`.

## Releases

The version is set in `gradle.properties` (currently `0.1.0`).

- **CI** (`.github/workflows/ci.yml`) runs on every push to `master` and every pull request. One job
  builds the project and runs the scenarios that need no client; a second runs the full suite against
  a real client under Xvfb and uploads its screenshots.
- **Releases** (`.github/workflows/release.yml`) run when a version tag is pushed. The workflow builds and
  tests with the version taken from the tag, then creates a GitHub release with the module jars and source
  jars attached, and notes generated from merged pull requests.

```bash
git tag v0.1.0
git push origin v0.1.0
```

Tags must look like `v1.2.3`; a suffix such as `v1.2.3-beta.1` makes a pre-release. Bump `version` in
`gradle.properties` afterwards so local builds don't keep the released version number.

**JitPack** builds any tag or commit on demand using `jitpack.yml`, which installs JDK 25 and publishes the
modules as `com.github.DaylightNebula.DiegeticSoup:diegetic-<module>:<tag>`. The first request for a new
tag takes a few minutes; open the [JitPack page](https://jitpack.io/#DaylightNebula/DiegeticSoup) and press
"Get it" to build ahead of time and see the log.

## License

[MIT](LICENSE.txt)
