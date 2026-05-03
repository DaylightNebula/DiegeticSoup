package dsh.diegetic.elements

import dsh.diegetic.interop.DItem
import net.kyori.adventure.text.Component
import org.joml.Matrix4f
import java.util.LinkedList

interface DiegeticElement {
    fun render(output: LinkedList<RenderedElement>, parent: Matrix4f)
}

sealed class RenderedElement {
    abstract val entityId: Int
    abstract val offset: Matrix4f

    data class Item(override val entityId: Int, val item: DItem, override val offset: Matrix4f): RenderedElement()
    data class Text(override val entityId: Int, val text: Component, override val offset: Matrix4f): RenderedElement()
}
