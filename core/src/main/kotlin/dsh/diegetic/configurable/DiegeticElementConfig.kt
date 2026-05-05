package dsh.diegetic.configurable

import dsh.diegetic.interop.DItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.joml.Quaternionf
import org.joml.Vector3f

@Serializable
data class DiegeticElementConfig(
    val position: ConfigVector = ConfigVector(0f, 0f, 0f),
    val rotation: ConfigRotation = ConfigRotation(0f, 0f, 0f),
    val scale: ConfigVector = ConfigVector(1f, 1f, 1f),
    val element: DiegeticElementTypeConfig = DiegeticElementTypeConfig.EmptyElementConfig(),
    val children: List<DiegeticElementConfig> = emptyList()
) {
    companion object {
        val json = Json { ignoreUnknownKeys = true }
        fun deserialize(text: String) = json.decodeFromString<DiegeticElementConfig>(text)
    }

    fun serialize() = json.encodeToString(this)
}

@Serializable
sealed class DiegeticElementTypeConfig {
    @Serializable
    @SerialName("item")
    data class ItemElementConfig(
        val type: String = "OAK_PLANKS",
        val customModelData: Int? = null
    ): DiegeticElementTypeConfig(), DItem

    @Serializable
    @SerialName("text")
    data class TextElementConfig(
        val text: String = "<red>Hello"
    ): DiegeticElementTypeConfig()

    @Serializable
    @SerialName("empty")
    class EmptyElementConfig: DiegeticElementTypeConfig()
}

@Serializable
data class ConfigVector(val x: Float, val y: Float, val z: Float) {
    fun toJoml(): Vector3f = Vector3f(x, y, z)
}

@Serializable
data class ConfigRotation(val yaw: Float, val pitch: Float, val roll: Float) {
    fun toJoml() = Quaternionf().rotateYXZ(yaw, pitch, roll)
}
