---
name: diegetic-soup
description: Build diegetic (in-world) Minecraft UIs with the Diegetic Soup library (packages dsh.diegetic.*) on Minestom or Paper servers. Use when a task involves showing text, items or panels in the world with display entities, DiegeticController / DSLController / diegetic { }, viewer or position controllers, flexbox layouts (FlexElement, FlexContainer, FlexText, FlexItem), clickable widgets (FlexButton, FlexToggle, FlexRadioGroup, FlexSlider), hover highlights, or adding the library to a project.
---

# Diegetic Soup

Diegetic Soup draws UIs in the world with **packet-only text and item display entities**: the server never
creates real entities. A UI is a **controller** that renders an **element** every tick and sends each viewer
only what changed since their last update, so a static UI costs nothing after it spawns.

| Topic | Reference |
|-------|-----------|
| Adding the library, registering the platform (Minestom / Paper) | [references/setup.md](references/setup.md) |
| Controllers, viewer and position controllers, elements, Java builders vs Kotlin DSL, animation | [references/controllers.md](references/controllers.md) |
| Flexbox layouts: containers, text, items, sizing, units | [references/flexbox.md](references/flexbox.md) |
| Clickable widgets, hover, reach, custom interactive elements | [references/interaction.md](references/interaction.md) |

## The shape of every UI

```kotlin
diegetic {                                                       // build + spawn (autoSpawn = true)
    viewers = InRadiusViewerController(MinestomLocation(pos), 32.0)  // who sees it
    position = StaticPositionController(MinestomLocation(pos))      // where it is (yaw/pitch turn it)
    flex(scale = 0.5f) {                                         // what is drawn
        direction(FlexDirection.COLUMN); padding(8); gap(6); background(0xE0101820)
        text("Hello")
        button("Click me") { event -> event.player.name() }
    }
}
```

```java
DSLController.create()
    .viewerController(new InRadiusViewerController(new MinestomLocation(pos), 32.0))
    .positionController(new StaticPositionController(new MinestomLocation(pos)))
    .element(FlexElement.create(FlexContainer.column().padding(8).gap(6)
        .child(FlexText.create("Hello"))
        .child(FlexButton.create("Click me", event -> {}))).scale(0.5f))
    .spawn();
```

In examples, `mm("<gold>text")` stands for `MiniMessage.miniMessage().deserialize("<gold>text")`.

## Facts to keep in mind

- **Register the platform first.** `DiegeticAPI.set(...)` must run before any element is created, because
  nodes allocate entity IDs from it. On Paper, PacketEvents must be initialised before
  `PaperDiegeticAPI.init(plugin)`, and IDs can only be allocated once a world has loaded.
- **Facing.** UIs face +z in their own space, and the controller's location yaw/pitch turns them (yaw 0
  faces south/+z, yaw 180 faces north). Text and backgrounds only render from the front, so put the UI
  between the player and the direction it faces: a player south of a yaw-0 UI, looking north, sees it.
- **Units.** Element transforms (`translate`, `scale`) are in blocks. Flex sizes are text pixels: 1 px =
  1/40 block, times the `FlexElement` scale. An item display at scale 1 draws a block item 1 block wide.
- **Keep nodes, mutate them.** Every node holds its entity IDs. Change a node's text, item or style and the
  next tick sends just that update; creating new nodes every tick respawns entities instead.
- **Threading.** Controllers tick on the server thread (Minestom scheduler / Bukkit main thread), and click
  handlers run there too. Change UI state from that thread.
- **Kotlin DSL = extension functions.** Import `dsh.diegetic.controller.*`, `dsh.diegetic.elements.*` and
  `dsh.diegetic.flex.*`; otherwise `viewers =`, `draw { }`, `text(...)` and friends don't resolve.
- **Widget state is shared** by everyone viewing a UI; handlers get the player who clicked. For per-player
  state, give each player their own controller (`StaticViewerController(listOf(player))`).
- **Reach.** Players can only hover and click within their own entity reach (`entity_interaction_range`,
  3 blocks by default; Minestom adds no creative bonus). Raise the attribute for comfortable panels.
- **Remove what you show.** `DiegeticAPI.get().removeController(controller)` destroys its entities for
  everyone; `RemoveEmptyViewerController` removes a UI automatically once nobody is watching.

## Common tasks

| Task | How |
|------|-----|
| Floating label or item | `diegetic { …; element { draw { text = mm("<gold>Hi") } } }` |
| Panel with layout | `flex { }` / `FlexElement.create(FlexContainer…)` ([flexbox.md](references/flexbox.md)) |
| Buttons, toggles, tabs, sliders | `button`, `toggle`, `radioGroup(style = RadioStyle.TABS)`, `slider` ([interaction.md](references/interaction.md)) |
| Follow a player's view | `PlayerPositionController(MinestomPlayer(p), Vector3f(x, y, forward))`, rotate the element 180° about Y so its front faces the player |
| Ride an entity | `parentEntity = MinestomEntity(entity)` (not clickable) |
| Animate | Supplier transforms: `scale { … }`, `translate { … }`, `rotation { … }`; smooth with `interpolationDuration` |
| Update text | keep the `FlexText`/`DSLRenderedElement` and call `text(...)` / set `text =` |

## When something looks wrong

| Symptom | Likely cause |
|---------|--------------|
| `DiegeticAPI is not initialized!` | Elements created before `DiegeticAPI.set(...)`. |
| Text invisible, items visible | Looking at the UI from behind: turn the location's yaw by 180°. |
| Panel huge or tiny | Flex px are 1/40 block; set `FlexElement.scale` (0.25–0.6 is typical). |
| Widget highlights but clicks do nothing / no highlight up close | Out of the player's reach, or the controller's `interactionRange` cap. |
| UI flickers / respawns every tick | New nodes or elements created inside a render or supplier instead of reusing them. |
| Unresolved `draw` / `text` / `viewers` in Kotlin | Missing `dsh.diegetic.*` DSL imports. |
