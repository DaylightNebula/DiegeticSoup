# Interaction

## Widgets

| Widget | Java | Kotlin DSL | Callback |
|--------|------|------------|----------|
| Button | `FlexButton.create("Save", e -> …)`, `create().child(…)` | `button("Save") { e -> }`, `button(onClick = { e -> }) { text("Save") }` | `onClick`, `onLeftClick`, `onRightClick` (`Consumer<ClickEvent>`) |
| Toggle | `FlexToggle.create("Subtitles").checked(true)` | `toggle(checked = true, onChange = { e, on -> }) { text("Subtitles") }` | `onChange(BiConsumer<ClickEvent, Boolean>)` |
| Radio / tabs | `FlexRadioGroup.<String>create().option("a", "A").selected("a")` | `radioGroup("a", RadioStyle.TABS, onChange = { e, v -> }) { option("a", "A"); option("b") { text("B") } }` | `onChange(BiConsumer<ClickEvent, V>)` |
| Slider | `FlexSlider.create(0f, 100f, 70f).step(5f)` | `slider(0f, 100f, 70f, onChange = { e, v -> }) { step(5f) }` | `onChange(BiConsumer<ClickEvent, Float>)` |

- Buttons, toggles and radio options are containers: give them any children, padding and backgrounds.
- All support `hoverBackground(argb)`, `disabled(true)`, `disabledBackground(argb)`; sliders have
  `trackColor`, `fillColor`, `thumbColor`, `thumbHoverColor`. Defaults look reasonable on a dark panel.
- `RadioStyle.TABS` lays options in a row and highlights the selected one (`tabBackground`,
  `tabSelectedBackground`); `DOTS` stacks them with a selection dot.
- Sliders: click to set; holding right click and moving the view along the track drags.
- `ClickEvent`: `player`, `type` (`LEFT`/`RIGHT`, also `isLeft`/`isRight`), `controller`, `x`/`y`/`width`/
  `height` (px within the widget, `x` may be null), `isDrag`.
- Widget state is shared by all viewers. Per-player state → one controller per player.

## How clicks work

Each enabled widget gets an invisible interaction entity (an axis-aligned hitbox around it) so the client
has something to click. When a click arrives, the server raycasts the player's view against the panel to
find the exact widget, so rotated and tilted UIs work. The same raycast runs every tick for hover.

- **Reach:** hover and clicks only work within the player's entity reach (`entity_interaction_range`,
  default 3; on Paper it should include vanilla's creative +2, on Minestom creative adds nothing). Clicks from further away are rejected
  on the server. `interactionRange(blocks)` on the controller caps it lower.
- **Hover mode:** `PER_VIEWER` (default) highlights only for the player looking; `SHARED` for everyone.
- **Not clickable:** UIs with `parentEntity`; disabled widgets (they spawn no hitbox).
- Handlers run on the server thread at the start of the next tick; changes they make render that tick.
  Exceptions in handlers are logged and don't stop the controller.
- On Minestom, `MinestomDiegeticAPI.init()` routes `ClientAttackPacket` (left) and
  `ClientInteractEntityPacket` (right, main hand) for UI hitboxes; Paper does the same through PacketEvents.

## Custom interactive elements

Implement `Interactive` (`click(event)`, optional `drag(event)`, `interactionEnabled`) and from your
element:

1. emit `RenderedElement.Interaction(entityId, rect, target)` in `render`, where `rect` maps the unit square
   (−0.5..0.5) onto the clickable rectangle in element space;
2. override `hitTest(ray, parent)`: intersect `ray` (in the space `parent` maps your element into) with your
   surface and return `Hit(distance, target, x, y, width, height)`;
3. read `context.isHovered(target)` in `render(output, parent, context)` to draw hover states.

Platforms report clicks through `InteractionRegistry.handle(player, entityId, ClickType)`.

## Testing UIs

The project's own tests drive a real client with NebsClient: look at a widget with `lookAt`, then
`attack()` (left) or `use()` (right). Put the player within reach (creative plus a raised
`entity_interaction_range`), and wait for the server to receive the new view before clicking.
