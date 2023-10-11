package io.github.wumpus.core

import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.nextInt

/** "Hunt the Wumpus" game driver. */
@Serializable
class Game private constructor(
    private val rooms: Map<Int, Room>,
    private var hunterRoomId: Int,
    private var wumpusRoomId: Int,
    /**
     * The maximum amount of rooms the hunter can shoot a crooked arrow through.
     */
    val maxArrowFlightRooms: Int,
    private var arrowsLeft: Int,
    private var isWumpusShot: Boolean = false,
    private var isHunterShot: Boolean = false
) {
    init {
        for ((id, room) in rooms) {
            require(room.neighborIds.isNotEmpty()) { "Each room must have at least one neighboring room" }
            for (neighborId in room.neighborIds) {
                val neighbor = requireNotNull(rooms[neighborId]) { "Room's neighbors must also be from this cave" }
                require(id in neighbor.neighborIds) { "Room must be a neighbor of its neighbors" }
            }
        }
        require(hunterRoomId in rooms.keys) { "The hunter must be in one of the rooms" }
        require(wumpusRoomId in rooms.keys) { "The Wumpus must be in one of the rooms" }
        require(rooms.getValue(hunterRoomId).hazard != Room.Hazard.BATS) { "The hunter cannot stay in a room with super bats" }
        require(maxArrowFlightRooms > 0) { "An arrow must be able to fly through at least a single room" }
    }

    /**
     * Creates a new game with the specified configuration.
     */
    constructor(
        /**
         * The rooms of the cave where the game takes place. Each room must all be connected (in both directions) to rooms
         * from this set.
         * */
        rooms: Set<Room>,
        /**
         * The ID of the room in which the hunter starts. This room must have no hazards and not be the same room in which
         * the Wumpus sleeps.
         */
        hunterRoomId: Int,
        /**
         * The ID of the room in which the Wumpus sleeps.
         */
        wumpusRoomId: Int,
        /**
         * The maximum amount of rooms the hunter can shoot a crooked arrow through.
         */
        maxArrowFlightRooms: Int,
        /**
         * The initial amount of arrows the hunter has.
         */
        initialArrowsNum: Int
    ) : this(rooms.associateBy { it.id }, hunterRoomId, wumpusRoomId, maxArrowFlightRooms, initialArrowsNum) {
        require(hunterRoomId != wumpusRoomId) { "The hunter and the Wumpus cannot start in the same room" }
        require(this.rooms.getValue(hunterRoomId).hazard == null) { "The hunter cannot start in a hazardous room" }
        require(initialArrowsNum > 0) { "The hunter must start with at least one arrow" }
    }

    private fun isFinished() = isHunterShot || isWumpusShot ||
            arrowsLeft == 0 ||
            hunterRoomId == wumpusRoomId ||
            rooms.getValue(hunterRoomId).hazard == Room.Hazard.PIT

    /**
     * Makes the hunter move to the specified neighboring room, returns the [MovementOutcome]s which constituted the
     * move.
     *
     * The returned list is non-empty. All but the last outcome are [MovementOutcome.SNATCHED_BY_BATS], and the last one
     * is any other outcome.
     */
    fun move(roomId: Int): List<MovementOutcome> {
        check(!isFinished()) { "Cannot move after the game has finished" }
        require(roomId in rooms.getValue(hunterRoomId).neighborIds) { "Can only move to hunter's neighboring rooms" }

        val events = mutableListOf<MovementOutcome>()
        var newHunterRoom = rooms.getValue(roomId)
        while (newHunterRoom.hazard == Room.Hazard.BATS && newHunterRoom.id != wumpusRoomId) {
            newHunterRoom = rooms.values.random()
            events.add(MovementOutcome.SNATCHED_BY_BATS)
        }
        when {
            newHunterRoom.id == wumpusRoomId -> events.add(MovementOutcome.EATEN_BY_WUMPUS)
            newHunterRoom.hazard == Room.Hazard.PIT -> events.add(MovementOutcome.FALLEN_INTO_PIT)
            newHunterRoom.hazard == null -> events.add(MovementOutcome.ENTERED)
        }

        hunterRoomId = newHunterRoom.id
        return events
    }

    /**
     * Makes the hunter shoot a crooked arrow through the specified adjacent rooms and returns the resulting
     * [ShootingOutcome].
     *
     * The first specified room should be a neighbor of the current hunter's room, and each next specified room should
     * be a neighbor of the previous one. If this is not the case for a particular pair of consecutively specified room
     * IDs, during this particular transition the arrow flies into a random neighboring room.
     */
    fun shoot(roomIds: Collection<Int>): ShootingOutcome {
        check(!isFinished()) { "Cannot shoot after the game has finished" }
        require(roomIds.isNotEmpty()) { "A shot must go through at least a single room" }
        require(roomIds.size <= maxArrowFlightRooms) { "Arrows cannot fly that far" }

        arrowsLeft -= 1

        var currentArrowRoom = rooms.getValue(hunterRoomId)
        var wrongDirections = false
        for (roomId in roomIds) {
            currentArrowRoom = rooms.getValue(
                if (!wrongDirections && roomId in currentArrowRoom.neighborIds) {
                    roomId
                } else {
                    wrongDirections = true
                    currentArrowRoom.neighborIds.random()
                }
            )
            if (currentArrowRoom.id == wumpusRoomId) {
                return ShootingOutcome.DEFEATED_WUMPUS
            }
            if (currentArrowRoom.id == hunterRoomId) {
                isHunterShot = true
                return ShootingOutcome.GOT_SHOT
            }
        }

        val wumpusNeighboringRoomIds = rooms.getValue(wumpusRoomId).neighborIds
        // If the index == WumpusNeighboringRoomIds.size the Wumpus stays asleep, otherwise it awakes and moves
        val newWumpusRoomIndex = Random.nextInt(0..wumpusNeighboringRoomIds.size)
        if (newWumpusRoomIndex < wumpusNeighboringRoomIds.size) {
            wumpusRoomId = wumpusNeighboringRoomIds.elementAt(newWumpusRoomIndex)
            if (wumpusRoomId == hunterRoomId) {
                return ShootingOutcome.EATEN_BY_WUMPUS
            }
        }

        return ShootingOutcome.MISSED
    }

    /**
     * Returns the room the hunter is currently in.
     */
    fun getHunterRoom(): Room = rooms.getValue(hunterRoomId)

    /**
     * Returns a descriptions of hazards currently surrounding the hunter.
     */
    fun getHunterSurroundings(): HunterSurroundings {
        val hunterRoom = rooms.getValue(hunterRoomId)
        val surroundingHazards = hunterRoom.neighborIds.mapTo(mutableSetOf()) { rooms.getValue(it).hazard }
        return HunterSurroundings(
            havePits = Room.Hazard.PIT in surroundingHazards,
            haveBats = Room.Hazard.BATS in surroundingHazards,
            haveWumpus = wumpusRoomId in hunterRoom.neighborIds
        )
    }

    data class HunterSurroundings(val havePits: Boolean, val haveBats: Boolean, val haveWumpus: Boolean)

    /**
     * Returns the amount of arrows the hunter currently has.
     */
    fun getArrowsLeft(): Int = arrowsLeft
}
