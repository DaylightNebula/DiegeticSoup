package dsh.diegetic.elements

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.DiegeticDsl
import dsh.diegetic.interaction.Hit
import dsh.diegetic.interaction.Ray
import dsh.diegetic.interaction.RenderContext
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.LinkedList
import java.util.function.Supplier

@DiegeticDsl
class DSLElement: DiegeticElement {
    companion object {
        @JvmStatic
        fun create() = DSLElement()
    }

    private val rendered: MutableList<DSLRenderedElement> = mutableListOf()
    private val children: MutableList<DiegeticElement> = mutableListOf()

    internal var translateFn: Supplier<Vector3f> = Supplier { Vector3f() }
        private set
    internal var rotationFn: Supplier<Quaternionf> = Supplier { Quaternionf() }
        private set
    internal var scaleFn: Supplier<Vector3f> = Supplier { Vector3f(1f) }
        private set

    fun translate(translate: Vector3f): DSLElement = translate { translate }
    fun translate(callback: Supplier<Vector3f>): DSLElement = apply { translateFn = callback }
    fun rotation(rotation: Quaternionf): DSLElement = rotation { rotation }
    fun rotation(callback: Supplier<Quaternionf>): DSLElement = apply { rotationFn = callback }
    fun scale(scale: Vector3f): DSLElement = scale { scale }
    fun scale(callback: Supplier<Vector3f>): DSLElement = apply { scaleFn = callback }

    val transform: Matrix4f
        get() = Matrix4f()
            .translate(translateFn.get())
            .rotate(rotationFn.get())
            .scale(scaleFn.get())

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) =
        render(output, parent, RenderContext.NONE)

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f, context: RenderContext) {
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

        children.forEach { it.render(output, Matrix4f(parentTransform), context) }
    }

    override fun hitTest(ray: Ray, parent: Matrix4f): Hit? {
        val parentTransform = Matrix4f(parent).mul(transform)
        return children.mapNotNull { it.hitTest(ray, parentTransform) }.minByOrNull { it.distance }
    }

    fun draw(element: DSLRenderedElement): DSLElement = apply { rendered.add(element) }
    fun child(child: DiegeticElement): DSLElement = apply { children.add(child) }
}

@DiegeticDsl
class DSLRenderedElement {
    companion object {
        @JvmStatic
        fun create() = DSLRenderedElement()
    }

    var entityId: Int = DiegeticAPI.get().nextEntityID()
        private set
    var renderedItem: DSLRenderedItem = DSLRenderedItem.None
        private set

    internal var currentTranslation: Vector3f = Vector3f()
        private set
    internal var currentRotation: Quaternionf = Quaternionf()
        private set
    internal var currentScale: Vector3f = Vector3f(1f)
        private set

    val transform: Matrix4f
        get() = Matrix4f()
            .translate(currentTranslation)
            .rotate(currentRotation)
            .scale(currentScale)

    fun entityId(entityId: Int): DSLRenderedElement = apply { this.entityId = entityId }
    fun item(item: DItem?): DSLRenderedElement = apply { renderedItem = item?.let { DSLRenderedItem.Item(it) } ?: DSLRenderedItem.None }
    fun text(text: Component?): DSLRenderedElement = apply { renderedItem = text?.let { DSLRenderedItem.Text(it) } ?: DSLRenderedItem.None }
    fun translation(translation: Vector3f): DSLRenderedElement = apply { currentTranslation = translation }
    fun rotation(rotation: Quaternionf): DSLRenderedElement = apply { currentRotation = rotation }
    fun scale(scale: Vector3f): DSLRenderedElement = apply { currentScale = scale }
}

sealed class DSLRenderedItem {
    class Item(val item: DItem): DSLRenderedItem()
    class Text(val text: Component): DSLRenderedItem()
    data object None: DSLRenderedItem()
}
