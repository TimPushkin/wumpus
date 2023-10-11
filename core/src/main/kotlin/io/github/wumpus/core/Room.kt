package io.github.wumpus.core

import kotlinx.serialization.Serializable

/** A room in a cave. */
@Serializable
data class Room internal constructor(
    /** Unique user-friendly ID of this room in the cave. */
    val id: Int,
    /** IDs of rooms to which there are direct tunnels from this one. */
    val neighborIds: Set<Int>,
    /** Describes what hazard this room contains, if any. */
    internal val hazard: Hazard?,
) {
    /** A non-moving danger a room can contain. */
    internal enum class Hazard {
        /** The room contains a bottomless pit. */
        PIT,

        /** The room contains super-bats. */
        BATS
    }
}
