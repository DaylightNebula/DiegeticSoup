# Controllers and elements

## DiegeticController

A controller = viewer controller + position controller + element, plus options:

| Option | Default | Meaning |
|--------|---------|---------|
| `parentEntity` | none | Mount every display on this entity (riding UIs; they can't be clicked). |
| `interpolationDuration` | 1 | Ticks the client blends a display to a new transform; 0 snaps. |
| `teleportDuration` | 1 | Ticks the client takes to move displays to a new position (0–59). |
| `hoverMode` | `PER_VIEWER` | `SHARED` shows everyone's hover highlights to everyone. |
| `interactionRange` | no cap | Caps hover/click distance below each player's own reach. |

Build one with the Java builder or the Kotlin DSL; both produce the same `DiegeticController`.

```java
DiegeticController c = DSLController.create()
    .viewerController(viewers).positionController(position).element(element)
    .interpolationDuration(3).hoverMode(HoverMode.SHARED)
    .spawn();          // or .build() to create without showing; DiegeticAPI.get().addController(c) shows it later
```

```kotlin
val c = diegetic(autoSpawn = true) {
    viewers = viewerController          // alias: viewerController = …
    position = positionController       // alias: positionController = …
    parentEntity = null
    interpolationDuration = 3
    hoverMode = HoverMode.SHARED
    element { … }                       // or flex { … }
}
```

Remove with `DiegeticAPI.get().removeController(c)`. `c.getViewers()`, `c.getRootPosition()`,
`c.getHovered(player)` and `c.raycast(player)` inspect it.

## Viewer controllers (who sees it)

| Class | Behaviour |
|-------|-----------|
| `StaticViewerController(listOf(p1, p2))` | Always these players. |
| `InRadiusViewerController(center, radius)` | Players within `radius` blocks of `center`. |
| `SinglePlayerNearbyViewerController(player, location, radius)` | One player; removes the UI when they walk away. |
| `RemoveEmptyViewerController(child)` | Wraps another; removes the UI once it has no viewers. |
| `DynamicViewerController { current, position -> ViewerTickResult(toAdd, toRemove) }` | Your own rule each tick. |

## Position controllers (where it is)

| Class | Behaviour |
|-------|-----------|
| `StaticPositionController(location)` | Fixed. The location's yaw/pitch rotate the whole UI. |
| `PlayerPositionController(player, offset, yawOverride, pitchOverride, useHeadLocation)` | In front of a player's view: `offset` is in look space (z forward, x to the player's left). The UI turns with the view, so its front faces away from the player: rotate the element 180° about Y. |
| `DynamicPositionController { location }` | Your own location each tick. |

Positions are compared by value, so an unmoving dynamic position sends nothing.

## DSL elements (free-form displays)

A `DSLElement` has a transform (translate → rotation → scale, in blocks), drawn displays and child
elements. Transforms take a value or a supplier evaluated every render (that's how animation works).

```kotlin
element {
    val start = System.currentTimeMillis()
    rotation { Quaternionf().rotateY((System.currentTimeMillis() - start) / 900f) }   // animated
    translate = Vector3f(0f, 1f, 0f)                                                  // fixed
    draw {
        item = MinestomItem(ItemStack.of(Material.DIAMOND_BLOCK))
        scale = Vector3f(0.5f)
    }
    child {
        translate = Vector3f(0f, 0.8f, 0f)
        draw { text = mm("<aqua>Label") }        // text displays get the vanilla translucent background
    }
}
```

```java
DSLElement.create()
    .rotation(() -> new Quaternionf().rotateY(System.currentTimeMillis() / 900f))
    .draw(DSLRenderedElement.create().item(new MinestomItem(ItemStack.of(Material.DIAMOND_BLOCK))).scale(new Vector3f(0.5f)))
    .child(DSLElement.create().translate(new Vector3f(0f, 0.8f, 0f))
        .draw(DSLRenderedElement.create().text(Component.text("Label"))));
```

A `DSLRenderedElement` draws one display: set `item` **or** `text` (setting one clears the other), plus its
own `translation`, `rotation` and `scale`. `DSLElement.flex { }` / `.child(FlexElement)` mixes in a flex panel.

Older building blocks still work: `StaticParentElement`, `StaticItemElement`, `StaticTextElement` and
`DynamicParentElement` (a per-child transform callback).

## Custom elements

Implement `DiegeticElement`:

```kotlin
class Clock : DiegeticElement {
    private val id = DiegeticAPI.get().nextEntityID()             // allocate once, reuse every render
    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        output.add(RenderedElement.Text(id, Component.text(LocalTime.now().toString()), Matrix4f(parent)))
    }
}
```

`RenderedElement.Text` takes `TextDisplayOptions(lineWidth, backgroundColor, alignment)`. Override
`render(output, parent, context)` to draw hover states, and `hitTest` to be clickable
(see [interaction.md](interaction.md)).

## Cost model

- Rendering happens every tick; only differences reach the network, per viewer.
- Animating a parent transform updates every display under it.
- A moving UI teleports every display each tick it moves; `parentEntity` avoids that for riding UIs.
