package dsh.diegetic.interop

import org.joml.Vector3f
import java.util.UUID

interface DPlayer {
    fun uuid(): UUID
    fun name(): String
    fun location(): DLocation
    fun eyeHeight(): Float
}