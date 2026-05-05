package dsh.diegetic.configurable

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.elements.DiegeticElement
import dsh.diegetic.elements.RenderedElement
import net.kyori.adventure.text.minimessage.MiniMessage
import org.joml.Matrix4f
import java.util.LinkedList

class ConfigurableDiegeticElement(
    val configId: String,
    var config: DiegeticElementConfig
): DiegeticElement {

    data class ConfigIntermediary(
        val entityId: Int,
        val config: DiegeticElementTypeConfig,
        val offset: Matrix4f,
        val children: List<ConfigIntermediary>
    ) {
        constructor(config: DiegeticElementConfig): this(
            entityId = DiegeticAPI.get().nextEntityID(),
            config = config.element,
            offset = Matrix4f()
                .scale(config.scale.toJoml())
                .rotate(config.rotation.toJoml())
                .translate(config.position.toJoml()),
            children = config.children.map { ConfigIntermediary(it) }
        )

        fun render(
            output: LinkedList<RenderedElement>,
            parent: Matrix4f
        ) {
            val transform = parent.mul(offset)
            val element = when (config) {
                is DiegeticElementTypeConfig.ItemElementConfig ->
                    RenderedElement.Item(entityId, config, transform)
                is DiegeticElementTypeConfig.TextElementConfig ->
                    RenderedElement.Text(entityId, MiniMessage.miniMessage().deserialize(config.text), transform)
                is DiegeticElementTypeConfig.EmptyElementConfig -> null
            }
            element?.let { output.push(it) }

            children.forEach { it.render(output, transform) }
        }
    }

    var intermediary = ConfigIntermediary(config)

    fun updateConfig(config: DiegeticElementConfig) {
        this.config = config
        this.intermediary = ConfigIntermediary(config)
    }

    override fun render(
        output: LinkedList<RenderedElement>,
        parent: Matrix4f
    ) = intermediary.render(output, parent)
}