package dsh.diegetic.test

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.DiegeticController
import dsh.diegetic.configurable.ConfigurableDiegeticElement
import dsh.diegetic.configurable.DiegeticElementConfig
import dsh.diegetic.elements.DynamicParentElement
import dsh.diegetic.elements.StaticItemElement
import dsh.diegetic.elements.StaticParentElement
import dsh.diegetic.elements.StaticTextElement
import dsh.diegetic.interop.MinestomEntity
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.PlayerPositionController
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.InRadiusViewerController
import dsh.diegetic.viewers.RemoveEmptyViewerController
import dsh.diegetic.viewers.SinglePlayerNearbyViewerController
import dsh.diegetic.viewers.StaticViewerController
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Matrix4f
import org.joml.Vector3f
import java.io.File
import kotlin.math.PI
import kotlin.math.cos

class TestCommand: Command("test") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("Usage: /test <#>")
        }

        var numberArgument = ArgumentType.Integer("number")

        addSyntax({ sender, context ->
            val number = context.get(numberArgument)
            if (number == null) return@addSyntax

            try {
                when (number) {
                    0 -> test0(sender as Player)
                    1 -> test1(sender as Player)
                    2 -> test2(sender as Player)
                    3 -> test3(sender as Player)
                    else -> sender.sendMessage("Invalid test number!")
                }
            } catch (e: Exception) { e.printStackTrace() }
        }, numberArgument)
    }

    fun test0(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = StaticViewerController(listOf(MinestomPlayer(player))),
                positionController = StaticPositionController(MinestomLocation(location)),
                element = StaticParentElement(
                    children = listOf(
                        StaticItemElement(
                            MinestomItem(ItemStack.of(Material.OAK_PLANKS)),
                            Matrix4f()
                        ),
                        StaticTextElement(
                            MiniMessage.miniMessage().deserialize("<red>Hello world!"),
                            Matrix4f().translate(0f, 1f, 0f)
                        )
                    )
                )
            )
        )
    }

    fun test1(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = RemoveEmptyViewerController(
                    child = InRadiusViewerController(MinestomLocation(location), 30.0)
                ),
                positionController = StaticPositionController(MinestomLocation(location)),
                element = DynamicParentElement(
                    children = listOf(
                        StaticItemElement(
                            MinestomItem(ItemStack.of(Material.OAK_PLANKS)),
                            Matrix4f()
                        )
                    ),
                    initialize = { System.currentTimeMillis() },
                    update = { startTime, _, _ ->
                        val timeSinceStart = System.currentTimeMillis() - startTime
                        val pulse = cos((timeSinceStart % 3000) / 1500.0 * PI) * 0.5f + 0.5f
                        val scale = pulse.toFloat() * 0.5f + 0.5f
                        Matrix4f().scale(scale, scale, scale)
                    }
                )
            )
        )
    }

    fun test2(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = SinglePlayerNearbyViewerController(MinestomPlayer(player), MinestomLocation(location), 30.0),
                positionController = PlayerPositionController(MinestomPlayer(player), Vector3f()),
                parentEntity = MinestomEntity(player),
                element = StaticTextElement(
                    MiniMessage.miniMessage().deserialize("<red>Hello world!"),
                    Matrix4f().scale(0.5f).rotateY(PI.toFloat()).translate(0f, -0.2f, -1f)
                )
            )
        )
    }

    fun test3(player: Player) {
        val configText = File("./test-minestom/configs/test1.json").readText()
        val config = DiegeticElementConfig.deserialize(configText)

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = SinglePlayerNearbyViewerController(MinestomPlayer(player), MinestomLocation(player.position), 30.0),
                positionController = PlayerPositionController(MinestomPlayer(player), Vector3f()),
                parentEntity = MinestomEntity(player),
                element = ConfigurableDiegeticElement("test1", config)
            )
        )
    }
}