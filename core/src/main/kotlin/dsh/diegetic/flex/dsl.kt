package dsh.diegetic.flex

import dsh.diegetic.controller.DSLController
import dsh.diegetic.elements.DSLElement
import dsh.diegetic.interaction.ClickEvent
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component

/** Builds a flex UI whose root is a container configured by [block]. */
fun flex(anchor: Anchor = Anchor.CENTER, scale: Float = 1f, block: FlexContainer.() -> Unit): FlexElement =
    FlexElement.create(FlexContainer.create().apply(block)).anchor(anchor).scale(scale)

/** Uses a flex UI as this controller's element. */
fun DSLController.flex(anchor: Anchor = Anchor.CENTER, scale: Float = 1f, block: FlexContainer.() -> Unit): DSLController =
    element(FlexElement.create(FlexContainer.create().apply(block)).anchor(anchor).scale(scale))

/** Adds a flex UI as a child of this element, so it follows the element's transform. */
fun DSLElement.flex(anchor: Anchor = Anchor.CENTER, scale: Float = 1f, block: FlexContainer.() -> Unit): DSLElement =
    child(FlexElement.create(FlexContainer.create().apply(block)).anchor(anchor).scale(scale))

fun FlexBox<*>.container(block: FlexContainer.() -> Unit): FlexContainer =
    FlexContainer.create().apply(block).also { child(it) }

fun FlexBox<*>.row(block: FlexContainer.() -> Unit): FlexContainer =
    container { direction(FlexDirection.ROW); block() }

fun FlexBox<*>.column(block: FlexContainer.() -> Unit): FlexContainer =
    container { direction(FlexDirection.COLUMN); block() }

fun FlexBox<*>.text(text: Component, block: FlexText.() -> Unit = {}): FlexText =
    FlexText.create(text).apply(block).also { child(it) }

fun FlexBox<*>.text(text: String, block: FlexText.() -> Unit = {}): FlexText =
    text(Component.text(text), block)

fun FlexBox<*>.item(item: DItem, block: FlexItem.() -> Unit = {}): FlexItem =
    FlexItem.create(item).apply(block).also { child(it) }

/** A clickable box; [onClick] runs for left and right clicks. */
fun FlexBox<*>.button(onClick: (ClickEvent) -> Unit = {}, block: FlexButton.() -> Unit): FlexButton =
    FlexButton.create().onClick(onClick).apply(block).also { child(it) }

/** A clickable box holding a text label. */
fun FlexBox<*>.button(label: String, onClick: (ClickEvent) -> Unit): FlexButton =
    FlexButton.create(label).onClick(onClick).also { child(it) }

/** A checkbox; [block] adds its label and other children after the indicator. */
fun FlexBox<*>.toggle(
    checked: Boolean = false,
    onChange: (ClickEvent, Boolean) -> Unit = { _, _ -> },
    block: FlexToggle.() -> Unit = {}
): FlexToggle = FlexToggle.create().checked(checked).onChange(onChange).apply(block).also { child(it) }

/** One choice out of several; add choices with [option]. */
fun <V> FlexBox<*>.radioGroup(
    selected: V? = null,
    style: RadioStyle = RadioStyle.DOTS,
    onChange: (ClickEvent, V) -> Unit = { _, _ -> },
    block: FlexRadioGroup<V>.() -> Unit
): FlexRadioGroup<V> = FlexRadioGroup.create<V>().style(style).onChange(onChange).apply(block).selected(selected).also { child(it) }

/** An option holding whatever [block] adds, such as text or an item. */
fun <V> FlexRadioGroup<V>.option(value: V, block: FlexRadioOption<V>.() -> Unit): FlexRadioOption<V> =
    addOption(value).apply(block)

/** A horizontal slider between [min] and [max]. */
fun FlexBox<*>.slider(
    min: Float = 0f,
    max: Float = 100f,
    value: Float = min,
    onChange: (ClickEvent, Float) -> Unit = { _, _ -> },
    block: FlexSlider.() -> Unit = {}
): FlexSlider = FlexSlider.create(min, max, value).onChange(onChange).apply(block).also { child(it) }

