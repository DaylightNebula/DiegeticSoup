package dsh.diegetic.demo

import dsh.diegetic.DiegeticAPI
import dsh.diegetic.MinestomDiegeticAPI
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.timer.TaskSchedule

/** Ground level of the demo world: the grass is y < 40, so players stand at y = 40. */
const val GROUND = 40

/** Where players spawn: in front of the middle station, facing it. */
val SPAWN = Pos(0.5, GROUND.toDouble(), 9.5, 180f, 0f)

/**
 * A demo server showing off Diegetic Soup. Run it with
 * `./gradlew :demo-minestom:runDemo [-Pdemo=all|basics|flexbox|widgets|hud|animation]`
 * and join `localhost` with Minecraft 26.3 (offline mode).
 */
fun main() {
    val selected = System.getProperty("demo", "all")
    startDemoServer(selected)
    println("Diegetic demo '$selected' running on localhost:25565")
}

/** Starts the server and builds the stations named by [selected] ("all" for the whole hub). */
fun startDemoServer(selected: String): InstanceContainer {
    val chosen = if (selected == "all") stations else stations.filter { it.id == selected }
    require(chosen.isNotEmpty()) { "Unknown demo '$selected'; pick one of all, ${stations.joinToString { it.id }}" }

    val server = MinecraftServer.init()
    val instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    instance.setChunkSupplier { i, x, z -> LightingChunk(i, x, z) }
    instance.setGenerator { unit -> unit.modifier().fillHeight(0, GROUND, Block.GRASS_BLOCK) }
    // a sunny midday, held there
    instance.time = 6000
    MinecraftServer.getSchedulerManager().scheduleTask({ instance.time = 6000 }, TaskSchedule.seconds(5), TaskSchedule.seconds(5))

    MinecraftServer.getGlobalEventHandler()
        .addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
            event.spawningInstance = instance
            event.player.respawnPoint = SPAWN
        }
        .addListener(PlayerSpawnEvent::class.java) { event ->
            val player = event.player
            player.gameMode = GameMode.CREATIVE
            // widgets need the player's reach; give everyone a little more than survival's 3 blocks
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).baseValue = 6.0
            player.sendMessage(Component.text("Welcome to the Diegetic Soup demo!"))
            chosen.forEach { player.sendMessage(mm("<gray>- <white>${it.title}</white>: ${it.subtitle}")) }
        }

    server.start("0.0.0.0", 25565)

    DiegeticAPI.set(MinestomDiegeticAPI().apply(MinestomDiegeticAPI::init))
    // stations are built once the spawn chunks exist
    instance.loadChunk(0, 0).thenRun {
        MinecraftServer.getSchedulerManager().scheduleNextTick {
            chosen.forEachIndexed { index, station ->
                val offset = if (chosen.size == 1) 0 else (index - chosen.size / 2) * STATION_SPACING
                val origin = Pos(offset + 0.5, GROUND.toDouble(), 0.5)
                station.buildSurroundings(origin, instance)
                station.build(origin, instance)
            }
        }
    }
    return instance
}

/** Where [station]'s origin is in the hub, matching [startDemoServer]. */
fun stationOrigin(station: Station, selected: List<Station> = stations): Pos {
    val index = selected.indexOf(station)
    val offset = if (selected.size == 1) 0 else (index - selected.size / 2) * STATION_SPACING
    return Pos(offset + 0.5, GROUND.toDouble(), 0.5)
}
