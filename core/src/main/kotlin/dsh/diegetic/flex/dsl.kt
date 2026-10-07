package dsh.diegetic.flex

import dsh.diegetic.controller.DSLController
import dsh.diegetic.elements.DSLElement
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

fun FlexContainer.container(block: FlexContainer.() -> Unit): FlexContainer =
    FlexContainer.create().apply(block).also { child(it) }

fun FlexContainer.row(block: FlexContainer.() -> Unit): FlexContainer =
    container { direction(FlexDirection.ROW); block() }

fun FlexContainer.column(block: FlexContainer.() -> Unit): FlexContainer =
    container { direction(FlexDirection.COLUMN); block() }

fun FlexContainer.text(text: Component, block: FlexText.() -> Unit = {}): FlexText =
    FlexText.create(text).apply(block).also { child(it) }

fun FlexContainer.text(text: String, block: FlexText.() -> Unit = {}): FlexText =
    text(Component.text(text), block)

fun FlexContainer.item(item: DItem, block: FlexItem.() -> Unit = {}): FlexItem =
    FlexItem.create(item).apply(block).also { child(it) }
