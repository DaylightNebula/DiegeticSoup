package dsh.diegetic.interop

import java.util.*

abstract class DPlayer {
    abstract fun uuid(): UUID
    abstract fun name(): String
    abstract fun location(): DLocation
    abstract fun eyeHeight(): Float

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is DPlayer -> other.uuid() == uuid()
            null -> false
            else -> super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return uuid().hashCode()
    }
}