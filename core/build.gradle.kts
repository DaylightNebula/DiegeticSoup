dependencies {
    // JOML types (Matrix4f, Vector3f, Quaternionf) are part of the public API
    api("org.joml:joml:1.10.8")
    // provided by Minestom and Paper
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
}
