# Diegetic Soup

Diegetic (in-world) UIs for [Minestom](https://minestom.net) and [Paper](https://papermc.io) Minecraft servers.
UIs are built from text and item display entities that are sent as packets only, so they cost the server
no real entities, and the library only sends packets when something a player can see actually changes.

- **Java builders and a Kotlin DSL** for the same API
- **Flexbox layout**: text, items and containers laid out like CSS flexbox, with wrapping text and backgrounds
- **Interaction**: buttons, toggles, radio groups and tabs, and sliders, with left and right clicks and
  per-viewer or shared hover highlights
- **Controllers** that decide who sees a UI and where it is: fixed, following a player, riding an entity
- **Efficient updates**: per-entity diffing, so a static UI sends nothing after it spawns, and smooth
  client-side interpolation for animations and movement
- Tested against a real Minecraft client with [NebsClient](https://github.com/DaylightNebula/NebsClient)

> **Status: 0.1.x, experimental.** The API may change between minor versions. Minestom is tested end to
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
    implementation("com.github.DaylightNebula.DiegeticSoup:diegetic-minestom:v0.1.2")
    // or, for a Paper plugin (shade it into your plugin jar):
    implementation("com.github.DaylightNebula.DiegeticSoup:diegetic-paper:v0.1.2")
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

## Interaction

Flex layouts can contain clickable widgets:

| Widget | What it does |
|--------|--------------|
| `FlexButton` | A box with `onClick`, `onLeftClick` and `onRightClick` handlers. |
| `FlexToggle` | A checkbox with a label; clicking flips it and calls `onChange(event, checked)`. |
| `FlexRadioGroup` | One choice out of several, drawn as dots (`RadioStyle.DOTS`) or tabs (`RadioStyle.TABS`); calls `onChange(event, value)`. |
| `FlexSlider` | A horizontal slider with `range`, `step` and `onChange(event, value)`. Click the track to set it, or hold right click and move along it to drag. |

Every widget is a flex node, so it is laid out like any other. Buttons, toggles and radio options are
containers, so they can hold any content. All of them support `hoverBackground`, `disabled` and
`disabledBackground`. Handlers get a `ClickEvent` with the player, `type` (`LEFT` or `RIGHT`), the
controller, and where on the widget they clicked.

```kotlin
flex(scale = 0.5f) {
    direction(FlexDirection.COLUMN); padding(8); gap(6); background(0xE0101820)

    radioGroup("audio", RadioStyle.TABS, onChange = { event, tab -> showTab(event.player, tab) }) {
        option("audio", "Audio"); option("video", "Video")
    }
    val volume = text("Volume: 70")
    slider(0f, 100f, 70f, onChange = { _, value -> volume.text("Volume: ${value.toInt()}") }) { step(5f) }
    toggle(checked = true) { text("Subtitles") }
    button("Save") { event -> if (event.isLeft) save(event.player) }
}
```

```java
FlexContainer.column().padding(8).gap(6)
    .child(FlexSlider.create(0f, 100f, 70f).step(5f).onChange((event, value) -> setVolume(value)))
    .child(FlexToggle.create("Subtitles").checked(true).onChange((event, on) -> setSubtitles(on)))
    .child(FlexButton.create("Save", event -> save(event.getPlayer())));
```

How it works and what to know:

- **Clicking:** each enabled widget gets an invisible interaction entity, so the client has something to
  click. The server then casts the player's view against the panel to find exactly which widget was
  clicked, so this works for panels at any yaw and pitch.
- **Reach:** players can only hover and click within their own entity reach: the `entity_interaction_range`
  attribute, 3 blocks by default. On Paper, creative mode adds 2 blocks as vanilla does; Minestom doesn't
  add that bonus, so set the attribute if creative players should reach further. Hover and clicks use the same rule,
  so anything highlighted can be clicked. Clicks from beyond reach are rejected on the server, so a
  modified client can't click from afar. `interactionRange(blocks)` on the controller can lower it further.
- **Hover:** with `hoverMode(HoverMode.PER_VIEWER)` (the default), each player only sees highlights for what
  they themselves are looking at. With `HoverMode.SHARED`, everyone sees what anyone is looking at.
- **Shared state:** widget state (checked, selected, value) is shared by everyone viewing a UI. For separate
  state per player, give each player their own controller.
- **Not supported yet:** clicks on UIs that ride an entity (`parentEntity`). Paper's input handling compiles
  but hasn't been tested on a live server yet.

Your own elements can be clickable too: implement `Interactive` and emit a `RenderedElement.Interaction`
for it, and override `DiegeticElement.hitTest` so the controller can find it under the player's view.

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

The version is set in `gradle.properties` (currently `0.1.2`).

- **CI** (`.github/workflows/ci.yml`) runs on every push to `master` and every pull request. One job
  builds the project and runs the scenarios that need no client; a second runs the full suite against
  a real client under Xvfb and uploads its screenshots.
- **Releases** (`.github/workflows/release.yml`) run when a version tag is pushed. The workflow builds and
  tests with the version taken from the tag, then creates a GitHub release with the module jars and source
  jars attached, notes generated from merged pull requests, and `jitpack-maven-repo.zip` for JitPack.

```bash
git tag v0.1.2
git push origin v0.1.2
```

Tags must look like `v1.2.3`; a suffix such as `v1.2.3-beta.1` makes a pre-release. Bump `version` in
`gradle.properties` afterwards so local builds don't keep the released version number.

**JitPack** serves the modules as `com.github.DaylightNebula.DiegeticSoup:diegetic-<module>:<tag>`. For a
release tag it doesn't build anything: [`.jitpack/install.sh`](.jitpack/install.sh) downloads the
`jitpack-maven-repo.zip` that the release workflow attached (waiting for the workflow if it is still
running), so JitPack serves exactly the artifacts CI built and tested. This works around JitPack's build
image currently failing to run Java ([jitpack#8096](https://github.com/jitpack/jitpack.io/issues/8096)).
Other versions, such as commit hashes, fall back to building with Gradle, which may fail while that issue
lasts. Once the release workflow has finished, open the [JitPack page](https://jitpack.io/#DaylightNebula/DiegeticSoup)
and press "Get it" so the first user doesn't wait.

## License

[MIT](LICENSE.txt)
