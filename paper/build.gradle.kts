dependencies {
    api(project(":core"))
    // provided by the server and the PacketEvents plugin
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
}
