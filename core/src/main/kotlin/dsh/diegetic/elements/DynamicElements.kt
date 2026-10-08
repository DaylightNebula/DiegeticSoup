package dsh.diegetic.elements

import dsh.diegetic.interaction.Hit
import dsh.diegetic.interaction.Ray
import dsh.diegetic.interaction.RenderContext
import org.joml.Matrix4f
import java.util.LinkedList

class DynamicParentElement<T: Any>(
    private val children: Collection<DiegeticElement>,
    initialize: () -> T,
    private val update: (data: T, element: DiegeticElement, elementIdx: Int) -> Matrix4f
): DiegeticElement {
    private val data = initialize()

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) =
        render(output, parent, RenderContext.NONE)

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f, context: RenderContext) {
        children.forEachIndexed { index, element ->
            val offset = update(data, element, index)
            element.render(output, Matrix4f(parent).mul(offset), context)
        }
    }

    override fun hitTest(ray: Ray, parent: Matrix4f): Hit? = children.mapIndexedNotNull { index, element ->
        element.hitTest(ray, Matrix4f(parent).mul(update(data, element, index)))
    }.minByOrNull { it.distance }
}


