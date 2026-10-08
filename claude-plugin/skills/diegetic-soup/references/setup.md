# Setup

## Dependency (JitPack)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.DaylightNebula.DiegeticSoup:diegetic-minestom:<tag>")   // Minestom
    implementation("com.github.DaylightNebula.DiegeticSoup:diegetic-paper:<tag>")      // or Paper (shade it)
}
```

`<tag>` is a release tag such as `v0.2.0`. Each platform module brings `diegetic-core` (and JOML, whose
`Matrix4f`/`Vector3f`/`Quaternionf` appear in the API) with it. Adventure, Minestom, Paper and PacketEvents
are expected on the classpath already (`compileOnly`). Requirements: Java 25, Minecraft 26.x.

## Registering the platform

Exactly once, before creating any element (nodes allocate entity IDs from the API):

**Minestom**

```kotlin
val server = MinecraftServer.init()
// ... instances, events ...
server.start("0.0.0.0", 25565)
DiegeticAPI.set(MinestomDiegeticAPI().apply(MinestomDiegeticAPI::init))
```

`init()` schedules the controller tick (every tick) and listens for clicks on UI hitboxes.

**Paper** (needs the PacketEvents plugin)

```kotlin
override fun onEnable() {
    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
    PacketEvents.getAPI().load()
    PacketEvents.getAPI().init()

    val api = PaperDiegeticAPI()
    api.init(this)
    DiegeticAPI.set(api)
}
```

Paper allocates entity IDs from the server's own counter (`Bukkit.getUnsafe().nextEntityId(world)`), so a
world must be loaded before elements are created. Paper support compiles and shares all of the core, but
has had less testing than Minestom.

## Wrapping platform objects

The core works with `DPlayer`, `DLocation`, `DItem` and `DEntity`. Wrap the platform's own types:

| Minestom | Paper |
|----------|-------|
| `MinestomPlayer(player)` | `BukkitPlayer(player)` |
| `MinestomLocation(pos)` (yaw/pitch included) | `BukkitLocation(location)` |
| `MinestomItem(ItemStack.of(Material.DIAMOND))` | `BukkitItem(itemStack)` |
| `MinestomEntity(entity)` | `BukkitEntity(entity)` |

`MinestomLocation.world()` is `""`, and `getPlayersInWorld` returns every online player, so Minestom
viewers aren't separated by instance.

## Packages

| Package | Contents |
|---------|----------|
| `dsh.diegetic` | `DiegeticAPI`, `MinestomDiegeticAPI`, `PaperDiegeticAPI`, `PacketAPI` |
| `dsh.diegetic.controller` | `DiegeticController`, `DSLController` builder, `diegetic { }` DSL |
| `dsh.diegetic.elements` | `DSLElement`, `DSLRenderedElement`, `element { }` DSL, `RenderedElement`, static/dynamic elements |
| `dsh.diegetic.viewers` / `.position` | viewer and position controllers |
| `dsh.diegetic.flex` | flexbox nodes, widgets, `FlexElement`, `flex { }` DSL, `MinecraftFont` |
| `dsh.diegetic.interaction` | `ClickEvent`, `ClickType`, `HoverMode`, `Interactive`, `InteractionRegistry` |
| `dsh.diegetic.interop` | `DPlayer`/`DLocation`/`DItem`/`DEntity` and platform wrappers |
