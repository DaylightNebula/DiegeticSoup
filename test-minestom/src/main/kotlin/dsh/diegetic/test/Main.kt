package dsh.diegetic.test

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.MinestomDiegeticAPI
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit

fun main() {
    // setup server
    val server = MinecraftServer.init()

    // create the instance
    val instanceManager = MinecraftServer.getInstanceManager()
    val instanceContainer = instanceManager.createInstanceContainer()

    // set the ChunkGenerator
    instanceContainer.setChunkSupplier { instance, cx, cz -> LightingChunk(instance, cx, cz) }
    instanceContainer.setGenerator { unit: GenerationUnit? ->
        unit!!.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
    }

    // add an event callback to specify the spawning instance (and the spawn position)
    val globalEventHandler = MinecraftServer.getGlobalEventHandler()
    globalEventHandler.addListener(
        AsyncPlayerConfigurationEvent::class.java
    ) { event: AsyncPlayerConfigurationEvent? ->
        val player: Player = event!!.player
        event.spawningInstance = instanceContainer
        player.respawnPoint = Pos(0.0, 42.0, 0.0)
    }

    // setup diegetics
    val api = MinestomDiegeticAPI()
    api.init("./test-minestom/configs")
    DiegeticAPI.set(api)

    // add command manager
    MinecraftServer.getCommandManager().register(TestCommand())

    // start server
    server.start("0.0.0.0", 25565)
}