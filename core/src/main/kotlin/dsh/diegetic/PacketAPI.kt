package dsh.diegetic

import dsh.diegetic.interop.DItem
import dsh.diegetic.interop.DLocation
import dsh.diegetic.interop.DPlayer
import net.kyori.adventure.text.Component
import org.joml.Matrix4f

interface PacketAPI {
    /**
     * Spawn an item display entity for the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param item      The item to display
     * @param position  The position of the entity
     * @param scale     The scale of the entity
     */
    fun spawnItemDisplay(viewers: Collection<DPlayer>, entityId: Int, item: DItem, location: DLocation, offset: Matrix4f)

    /**
     * Spawn a text display entity for the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param text      The text to display
     * @param position  The position of the entity
     * @param scale     The scale of the entity
     */
    fun spawnTextDisplay(viewers: Collection<DPlayer>, entityId: Int, text: Component, location: DLocation, offset: Matrix4f)

    /**
     * Updates the offset of a display entity.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param offset    The new offset of the entity
     */
    fun updateDisplayOffset(viewers: Collection<DPlayer>, entityId: Int, offset: Matrix4f);

    /**
     * Update an item display entity for the given list of viewers with a particular item.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param item      The item to display
     */
    fun updateItemDisplay(viewers: Collection<DPlayer>, entityId: Int, item: DItem)

    /**
     * Update a text display entity for the given list of viewers with a particular text.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param text      The text to display
     */
    fun updateTextDisplay(viewers: Collection<DPlayer>, entityId: Int, text: Component)

    /**
     * Move an entity to a new position for the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param position  The position to move the entity to
     */
    fun moveEntity(viewers: Collection<DPlayer>, entityId: Int, location: DLocation)

    /**
     * Scale an entity to a new scale for the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     * @param scale     The scale to scale the entity to
     */
    fun scaleEntity(viewers: Collection<DPlayer>, entityId: Int, scale: Float)

    /**
     * Remove an entity from the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityId  The entity ID
     */
    fun removeEntity(viewers: Collection<DPlayer>, entityId: Int)

    /**
     * Remove multiple entities from the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param entityIds The entity IDs to remove
     */
    fun removeEntities(viewers: Collection<DPlayer>, entityIds: Collection<Int>)

    /**
     * Update the parent of a list of entities for the given list of viewers.
     *
     * @param viewers   The viewers to show the entity too
     * @param parentId  The new parent ID
     */
    fun setEntityPassengers(viewers: Collection<DPlayer>, parentId: Int, entityIds: Collection<Int>)
}