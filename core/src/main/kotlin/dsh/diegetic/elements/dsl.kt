package dsh.diegetic.elements

import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Quaternionf
import org.joml.Vector3f

var DSLElement.translate: Vector3f
    get() = translateFn.get()
    set(value) { translate(value) }
var DSLElement.rotation: Quaternionf
    get() = rotationFn.get()
    set(value) { rotation(value) }
var DSLElement.scale: Vector3f
    get() = scaleFn.get()
    set(value) { scale(value) }

fun DSLElement.draw(callback: DSLRenderedElement.() -> Unit): DSLElement = draw(DSLRenderedElement.create().apply(callback))

fun DSLElement.child(callback: DSLElement.() -> Unit): DSLElement = child(element(callback))

var DSLRenderedElement.translation: Vector3f
    get() = currentTranslation
    set(value) { translation(value) }
var DSLRenderedElement.rotation: Quaternionf
    get() = currentRotation
    set(value) { rotation(value) }
var DSLRenderedElement.scale: Vector3f
    get() = currentScale
    set(value) { scale(value) }

var DSLRenderedElement.item: DItem?
    get() = (renderedItem as? DSLRenderedItem.Item)?.item
    set(value) { item(value) }

var DSLRenderedElement.text: Component?
    get() = (renderedItem as? DSLRenderedItem.Text)?.text
    set(value) { text(value) }

fun element(callback: DSLElement.() -> Unit): DSLElement = DSLElement.create().apply(callback)
