package dsh.diegetic.test

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.controller.DiegeticController
import dsh.diegetic.controller.diegetic
import dsh.diegetic.elements.DynamicParentElement
import dsh.diegetic.elements.StaticItemElement
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
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class TestCommand: Command("test") {
    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("Usage: /test <#>")
        }

        var numberArgument = ArgumentType.Integer("number")

        addSyntax({ sender, context ->
            val number = context.get(numberArgument)
            if (number == null) return@addSyntax
            when (number) {
                0 -> test0(sender as Player)
                1 -> test1(sender as Player)
                2 -> test2(sender as Player)
                3 -> test3(sender as Player)
                4 -> test4(sender as Player)
                else -> sender.sendMessage("Invalid test number!")
            }
        }, numberArgument)
    }

    fun test0(player: Player) {
        val location = player.position
            .withY { it + player.eyeHeight }
            .withPitch { 0.0 }

        diegetic {
            viewers = StaticViewerController(listOf(MinestomPlayer(player)))
            position = StaticPositionController(MinestomLocation(location))

            element {
                val startTime = System.currentTimeMillis()

                scale {
                    val time = System.currentTimeMillis()
                    val diff = (time - startTime) % 2500 / 2500f
                    val scale = sin(diff * 2f * 3.1415f)
                    Vector3f(scale * 0.25f + 0.5f)
                }

                draw {
                    item = MinestomItem(ItemStack.of(Material.OAK_PLANKS))
                }

                draw {
                    text = MiniMessage.miniMessage().deserialize("<red>Hello world!")
                    translation = Vector3f(0f, 1f, 0f)
                }

                draw {
                    text = MiniMessage.miniMessage().deserialize("<red>Hello world!")
                    translation = Vector3f(0f, 1f, 0f)
                    rotation = Quaternionf().rotateY(Math.PI.toFloat())
                }
            }
        }
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
        player.instance.setBlock(player.position.blockX(), player.position.blockY(), player.position.blockZ(), Block.YELLOW_CONCRETE)

        val location = Pos(player.position.blockX().toDouble(), player.position.blockY().toDouble(), player.position.blockZ().toDouble())
            .withX { it + 0.5 }
            .withY { it + 1.025 }
            .withZ { it + 1.0 }
            .withPitch { -90.0 }
            .withYaw { 0.0 }

        DiegeticAPI.get().addController(
            DiegeticController(
                viewerController = SinglePlayerNearbyViewerController(MinestomPlayer(player), MinestomLocation(location), 30.0),
                positionController = StaticPositionController(MinestomLocation(location)),
                element = StaticTextElement(
                    text = MiniMessage.miniMessage().deserialize("W"),
                    offset = Matrix4f().scale(2.15f)
                )
            )
        )
    }

    fun test4(player: Player) {
        val location = Pos(player.position.blockX().toDouble(), player.position.blockY().toDouble(), player.position.blockZ().toDouble())
            .withPitch { 0.0 }

    }
}