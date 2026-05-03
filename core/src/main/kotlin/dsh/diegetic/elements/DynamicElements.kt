package dsh.diegetic.elements

import org.joml.Matrix4f
import java.util.LinkedList

class DynamicParentElement<T: Any>(
    private val children: Collection<DiegeticElement>,
    initialize: () -> T,
    private val update: (data: T, element: DiegeticElement, elementIdx: Int) -> Matrix4f
): DiegeticElement {
    private val data = initialize()

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        children.forEachIndexed { index, element ->
            val offset = update(data, element, index)
            element.render(output, Matrix4f(parent).mul(offset))
        }
    }
}


