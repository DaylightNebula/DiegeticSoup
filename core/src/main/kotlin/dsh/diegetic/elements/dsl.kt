package dsh.diegetic.elements

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.LinkedList

class DSLElement: DiegeticElement {

    private val rendered: MutableList<DSLRenderedElement> = mutableListOf()
    private val children: MutableList<DiegeticElement> = mutableListOf()

    private var translateFn: () -> Vector3f = { Vector3f() }
    private var rotationFn: () -> Quaternionf = { Quaternionf() }
    private var scaleFn: () -> Vector3f = { Vector3f(1f) }

    var translate: Vector3f
        get() = translateFn()
        set(value) { translateFn = { value }  }
    var rotation: Quaternionf
        get() = rotationFn()
        set(value) { rotationFn = { value }  }
    var scale: Vector3f
        get() = scaleFn()
        set(value) { scaleFn = { value }  }

    fun translate(callback: () -> Vector3f) { this.translateFn = callback }
    fun rotation(callback: () -> Quaternionf) { this.rotationFn = callback }
    fun scale(callback: () -> Vector3f) { this.scaleFn = callback }

    val transform: Matrix4f
        get() = Matrix4f()
            .translate(translate)
            .rotate(rotation)
            .scale(scale)

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        val parentTransform = Matrix4f(parent).mul(transform)

        output.addAll(
            rendered.mapNotNull { element ->
                val item = element.renderedItem

                when (item) {
                    is DSLRenderedItem.Text ->
                        RenderedElement.Text(
                            entityId = element.entityId,
                            text = item.text,
                            offset = Matrix4f(parentTransform)
                                .mul(element.transform)
                        )
                    is DSLRenderedItem.Item ->
                        RenderedElement.Item(
                            entityId = element.entityId,
                            item = item.item,
                            offset = Matrix4f(parentTransform)
                                .mul(element.transform)
                        )
                    is DSLRenderedItem.None -> null
                }
            }
        )

        children.forEach { it.render(output, Matrix4f(parentTransform)) }
    }

    fun draw(callback: DSLRenderedElement.() -> Unit) {
        val element = DSLRenderedElement()
        element.callback()
        rendered.add(element)
    }

    fun child(callback: DSLElement.() -> Unit) {
        val element = DSLElement()
        element.callback()
        children.add(element)
    }

    fun child(child: DiegeticElement) = children.add(child)
}

class DSLRenderedElement {
    var entityId: Int = DiegeticAPI.get().nextEntityID()
    var renderedItem: DSLRenderedItem = DSLRenderedItem.None
        private set

    var translation: Vector3f = Vector3f()
    var rotation: Quaternionf = Quaternionf()
    var scale: Vector3f = Vector3f(1f)

    val transform: Matrix4f
        get() = Matrix4f()
            .translate(translation)
            .rotate(rotation)
            .scale(scale)

    var item: DItem?
        get() = (renderedItem as? DSLRenderedItem.Item)?.item
        set(value) { renderedItem = value?.let { DSLRenderedItem.Item(it) } ?: DSLRenderedItem.None }

    var text: Component?
        get() = (renderedItem as? DSLRenderedItem.Text)?.text
        set(value) { renderedItem = value?.let { DSLRenderedItem.Text(it) } ?: DSLRenderedItem.None }
}

sealed class DSLRenderedItem {
    class Item(val item: DItem): DSLRenderedItem()
    class Text(val text: Component): DSLRenderedItem()
    data object None: DSLRenderedItem()
}

fun element(callback: DSLElement.() -> Unit): DSLElement {
    val element = DSLElement()
    element.callback()
    return element
}
