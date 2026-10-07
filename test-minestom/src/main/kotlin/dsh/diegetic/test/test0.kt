package dsh.diegetic.test

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.MinestomDiegeticAPI
import dsh.diegetic.controller.diegetic
import dsh.diegetic.interop.MinestomItem
import dsh.diegetic.interop.MinestomLocation
import dsh.diegetic.interop.MinestomPlayer
import dsh.diegetic.position.StaticPositionController
import dsh.diegetic.viewers.StaticViewerController
import dsh.nebsclient.api.MoveDirection
import dsh.nebsclient.api.NebsClient
import dsh.nebsclient.core.ClaudeSkill
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.io.path.absolutePathString
import kotlin.math.sin

fun main() = startTemplateServer {
    DiegeticAPI.set(MinestomDiegeticAPI().apply(MinestomDiegeticAPI::init))

    val client = NebsClient.running().firstOrNull()
        ?: NebsClient { waitForReady = true }

    client.connect("localhost", 25565)
    Thread.sleep(1500)
    client.move(MoveDirection.BACK, 10)

    val player = MinecraftServer.getConnectionManager().onlinePlayers.first()
    val location = Pos(0.0, 41.5, 5.0, 0f, 0f)

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

    Thread.sleep(1500)
    val ss = client.screenshot("test")
    println("Path ${ss.path.absolutePathString()}")
    client.exit()
    MinecraftServer.stopCleanly()
}