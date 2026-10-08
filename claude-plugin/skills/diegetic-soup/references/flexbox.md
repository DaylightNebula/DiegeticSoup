# Flexbox layouts

`FlexElement` lays out a tree of nodes the way CSS flexbox does (checked against a real browser) and
renders it with text and item displays. The panel lies in the element's XY plane facing +z.

## Nodes

| Node | Java | Kotlin DSL (inside a box) | Notes |
|------|------|---------------------------|-------|
| Container | `FlexContainer.create()/row()/column()` | `container { }`, `row { }`, `column { }` | Optional `background(argb)`. |
| Text | `FlexText.create("…")` / `create(component)` | `text("…") { }` | Wraps to its box like the client does; `textBackground`, `textAlign` (LEFT default). |
| Item | `FlexItem.create(MinestomItem(…))` | `item(dItem) { size(24, 24) }` | Natural size 16×16; drawn centred and scaled to fit. |
| Widgets | `FlexButton`, `FlexToggle`, `FlexRadioGroup`, `FlexSlider` | `button`, `toggle`, `radioGroup`, `slider` | See [interaction.md](interaction.md). |

```kotlin
flex(anchor = Anchor.CENTER, scale = 0.5f) {           // root is a container
    direction(FlexDirection.COLUMN); width(200); padding(8); gap(6); background(0xE0101820)
    row {
        justifyContent(JustifyContent.SPACE_BETWEEN); alignItems(AlignItems.CENTER)
        text(mm("<gold><bold>Shop")); text(mm("<gray>3 items"))
    }
    text("Long text wraps to the panel's width.")
    row { gap(6); repeat(3) { column { grow(1f); padding(4); background(0x30FFFFFF); item(diamond) { size(24, 24) } } } }
}
```

```java
FlexElement.create(
    FlexContainer.column().width(200).padding(8).gap(6).background(0xE0101820)
        .child(FlexText.create("Long text wraps to the panel's width."))
        .child(FlexItem.create(new MinestomItem(ItemStack.of(Material.DIAMOND))).size(24, 24))
).scale(0.5f).anchor(Anchor.CENTER);
```

Kotlin: setters on boxes are the fluent methods themselves (`gap(6)`, `direction(...)`); Int overloads
exist for the common ones, others take Floats (`grow(1f)`, `padding(3f, 8f)`).

## Style properties

| Property | Methods |
|----------|---------|
| Size | `width`, `height`, `size(w, h)`, `minWidth`, `minHeight`, `maxWidth`, `maxHeight` — px, or `Length.percent(50f)` of the parent's inner size |
| Flex item | `grow`, `shrink` (default 1), `basis`, `flex(grow, shrink, basis)`, `alignSelf`, `margin(...)` |
| Container | `direction` (ROW, COLUMN, *_REVERSE), `wrap` (NO_WRAP, WRAP, WRAP_REVERSE), `justifyContent`, `alignItems` (STRETCH default), `alignContent`, `gap(px)` / `gap(row, column)`, `padding(...)`, `background(argb)` |
| Element | `FlexElement.anchor(Anchor.X)` (which point of the root sits on the origin, default CENTER), `scale` (block size multiplier), `depthStep` (z gap between nesting levels) |

Not supported: `margin: auto`, `order`, baseline alignment, `aspect-ratio`, percentage padding.

## Units and sizing

- 1 px = 1/40 block at scale 1. A 200 px panel at `scale(0.5f)` is 2.5 blocks wide.
- Text measures with the vanilla default font (ASCII widths, bold +1 px, 10 px per line); non-ASCII
  characters are estimated at 6 px. A resource pack that changes the default font breaks measurements.
- A text node's box is its widest line + 1 px (the display background's padding) by lines × 10 px.
- Colours are ARGB: `0xE0101820` is a mostly opaque dark blue. In Kotlin pass `Long` literals freely
  (`background(0xE0101820)`); Java ints like `0xE0101820` work too.
- Translucent backgrounds stack and let the world show through; use opaque colours (`0xFF…`) for panels
  that must look the same everywhere.

## Updating

The layout runs every render. Keep references to nodes and change them (`label.text("Score: 3")`,
`container.removeChild(node)`, `box.background(color)`): the panel reflows and only changed entities are
updated. `FlexElement.layout()` returns each node's `Box` (px, y down), and `FlexLayout.compute(root)` does
the same without an element.
