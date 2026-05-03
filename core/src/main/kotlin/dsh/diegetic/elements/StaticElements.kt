package dsh.diegetic.elements

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList

class StaticParentElement(
    val children: Collection<DiegeticElement>,
    val offset: Matrix4f = Matrix4f()
): DiegeticElement {
    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        children.forEach { it.render(output, Matrix4f(parent).mul(offset)) }
    }
}

class StaticItemElement(
    val item: DItem,
    val offset: Matrix4f = Matrix4f()
): DiegeticElement {
    val entityId = DiegeticAPI.get().nextEntityID()

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        output.push(RenderedElement.Item(entityId, item, Matrix4f(parent).mul(offset)))
    }
}

class StaticTextElement(
    val text: Component,
    val offset: Matrix4f = Matrix4f()
): DiegeticElement {
    val entityId = DiegeticAPI.get().nextEntityID()

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) {
        output.push(RenderedElement.Text(entityId, text, Matrix4f(parent).mul(offset)))
    }
}
