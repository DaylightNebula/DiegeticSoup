package dsh.diegetic.elements

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.interaction.Hit
import dsh.diegetic.interaction.Ray
import dsh.diegetic.interaction.RenderContext
import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList

class StaticParentElement(
    val children: Collection<DiegeticElement>,
    val offset: Matrix4f = Matrix4f()
): DiegeticElement {
    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f) =
        render(output, parent, RenderContext.NONE)

    override fun render(output: LinkedList<RenderedElement>, parent: Matrix4f, context: RenderContext) {
        children.forEach { it.render(output, Matrix4f(parent).mul(offset), context) }
    }

    override fun hitTest(ray: Ray, parent: Matrix4f): Hit? =
        children.mapNotNull { it.hitTest(ray, Matrix4f(parent).mul(offset)) }.minByOrNull { it.distance }
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
