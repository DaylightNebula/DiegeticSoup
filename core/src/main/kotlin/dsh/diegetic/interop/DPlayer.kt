package dsh.diegetic.interop

import java.util.*

abstract class DPlayer {
    abstract fun uuid(): UUID
    abstract fun name(): String
    abstract fun location(): DLocation
    abstract fun eyeHeight(): Float

    /**
     * How far, in blocks, the player can reach to click entities: the `entity_interaction_range`
     * attribute (including any creative bonus the platform applies). UIs only show hovers and accept clicks within it.
     */
    open fun interactionRange(): Float = DEFAULT_INTERACTION_RANGE

    companion object {
        /** Vanilla's entity reach in survival. */
        const val DEFAULT_INTERACTION_RANGE = 3f
    }

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