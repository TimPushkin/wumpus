package io.github.wumpus.core

enum class MovementOutcome {
    /** The hunter moved and ended up in the specified room with the specified neighbors. */
    ENTERED,

    /** The hunter moved into a room with super-bats and was taken to another room. */
    SNATCHED_BY_BATS,

    /** The hunter moved into a room with a bottomless pit and fell into it. */
    FALLEN_INTO_PIT,

    /** The hunter moved into the Wumpus' room and was eaten. */
    EATEN_BY_WUMPUS
}

enum class ShootingOutcome {
    /** The hunter missed and none of the other shooting events happened. */
    MISSED,

    /** The hunter shot themselves. */
    GOT_SHOT,

    /** The hunter shot the Wumpus. */
    DEFEATED_WUMPUS,

    /** The hunter missed a shot and awoken the Wumpus who found and ate him. */
    EATEN_BY_WUMPUS
}
