package io.github.wumpus.configs

import io.github.wumpus.core.Game
import io.github.wumpus.core.Room
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * Creates a randomly generated game with a configuration mirroring the classic 1973 "Hunt the Wumpus" gameplay:
 * - The cave is a dodecahedron, i.e. there are 20 rooms each of which has three tunnels leading to other rooms
 * - Two rooms have bottomless pits and two other have super-bats
 * - Ihe hunter initially has five arrows each of which can through up to five rooms
 */
fun generateClassicGame(): Game {
    // 20 rooms total
    val roomIds = 1..20
    // 1 has the Wumpus (it can also a hazard)
    val wumpusRoomId = Random.nextInt(roomIds)
    // 4 distinct rooms have hazards (2 with a pit and 2 with bats)
    val (roomWithPitIds, roomWithBatsIds) = generateSequence { Random.nextInt(roomIds) }.distinct()
        .take(4).chunked(2)
        .toList().run { elementAt(0) to elementAt(1) }
    // 1 has the hunter (it cannot have the Wumpus or a hazard)
    val hunterRoomId = generateSequence { Random.nextInt(roomIds) }.filterNot {
        it == wumpusRoomId || it in roomWithPitIds || it in roomWithBatsIds
    }.first()

    // The map of the cave: https://en.wikipedia.org/wiki/Hunt_the_Wumpus#/media/File:Hunt_the_Wumpus_map.svg
    val neighbors = mapOf(
        1 to setOf(2, 5, 8),
        2 to setOf(1, 3, 10),
        3 to setOf(2, 4, 12),
        4 to setOf(3, 5, 14),
        5 to setOf(1, 4, 6),
        6 to setOf(5, 7, 15),
        7 to setOf(6, 8, 17),
        8 to setOf(1, 7, 9),
        9 to setOf(8, 10, 18),
        10 to setOf(2, 9, 11),
        11 to setOf(10, 12, 19),
        12 to setOf(3, 11, 13),
        13 to setOf(12, 14, 20),
        14 to setOf(4, 13, 15),
        15 to setOf(6, 14, 16),
        16 to setOf(15, 17, 20),
        17 to setOf(7, 16, 18),
        18 to setOf(9, 17, 19),
        19 to setOf(11, 18, 20),
        20 to setOf(13, 16, 19),
    )

    val rooms = roomIds.mapTo(mutableSetOf()) { id ->
        Room(
            id = id,
            hazard = when (id) {
                in roomWithPitIds -> Room.Hazard.PIT
                in roomWithBatsIds -> Room.Hazard.BATS
                else -> null
            },
            neighborIds = neighbors.getValue(id)
        )
    }

    return Game(rooms, hunterRoomId, wumpusRoomId, maxArrowFlightRooms = 5, initialArrowsNum = 5)
}
