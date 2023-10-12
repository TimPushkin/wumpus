package io.github.wumpus.console

import io.github.wumpus.configs.generateClassicGame
import io.github.wumpus.core.Game
import io.github.wumpus.core.MovementOutcome
import io.github.wumpus.core.Room
import io.github.wumpus.core.ShootingOutcome

fun play(game: Game = generateClassicGame()) {
    while (true) {
        game.getHunterSurroundings().describe()

        val currentRoom = game.getHunterRoom()
        currentRoom.describe()

        when (readShootOrMove()) {
            ShootOrMove.SHOOT -> {
                val roomsForShooting = readRoomsForShooting(game.maxArrowFlightRooms)
                when (game.shoot(roomsForShooting)) {
                    ShootingOutcome.MISSED -> {
                        println("Missed!")
                        if (game.getArrowsLeft() == 0) {
                            printDefeatMessage("You are out of arrows — the Wumpus will get you eventually!")
                            return
                        }
                    }
                    ShootingOutcome.GOT_SHOT -> {
                        printDefeatMessage("Ouch! You shot yourself!")
                        return
                    }
                    ShootingOutcome.DEFEATED_WUMPUS -> {
                        printVictoryMessage("You shot the Wumpus!")
                        return
                    }
                    ShootingOutcome.EATEN_BY_WUMPUS -> {
                        printDefeatMessage("You missed and the Wumpus found you and ate you!")
                        return
                    }
                }
            }
            ShootOrMove.MOVE -> {
                val roomToMove = readRoomToMove(currentRoom.neighborIds)
                for (outcome in game.move(roomToMove)) {
                    when (outcome) {
                        MovementOutcome.SNATCHED_BY_BATS -> println("Super bats snatch you to another room!")
                        MovementOutcome.ENTERED -> println()  // Continue to the next move
                        MovementOutcome.FALLEN_INTO_PIT -> {
                            printDefeatMessage("AAAaaah... You fell into a pit!")
                            return
                        }
                        MovementOutcome.EATEN_BY_WUMPUS -> {
                            printDefeatMessage("You went right into the Wumpus' room and it ate you!")
                            return
                        }
                    }
                }
            }
        }
    }
}

private fun Game.HunterSurroundings.describe() {
    if (haveWumpus) {
        println("I smell the Wumpus!")
    }
    if (havePits) {
        println("I feel a draft!")
    }
    if (haveBats) {
        println("Bats nearby!")
    }
}

private fun Room.describe() {
    println("You are in room $id.")
    println("Tunnels lead to: ${neighborIds.joinToString(separator = " ")}")
}

private fun readShootOrMove(): ShootOrMove {
    while (true) {
        print("Shoot or Move (S/M)? ")
        val input = readln()
        if (input.equals("s", ignoreCase = true)) {
            return ShootOrMove.SHOOT
        }
        if (input.equals("m", ignoreCase = true)) {
            return ShootOrMove.MOVE
        }
    }
}

enum class ShootOrMove { SHOOT, MOVE }

private fun readRoomsForShooting(maxRoomsForShooting: Int): List<Int> {
    while (true) {
        print("Through which rooms (1-5 space-separated room numbers)? ")
        val nullableNumbers = readln().split(" ").map { it.toIntOrNull() }
        val numbers = nullableNumbers.filterNotNull()
        if (nullableNumbers.size == numbers.size && numbers.size in 1..maxRoomsForShooting) {
            return numbers
        }
    }
}

private fun readRoomToMove(possibleRooms: Set<Int>): Int {
    while (true) {
        print("Where to? ")
        val number = readln().toIntOrNull() ?: continue
        if (number in possibleRooms) {
            return number
        }
    }
}

private fun printDefeatMessage(description: String) {
    println(description)
    println()
    println("You lost!")
}

private fun printVictoryMessage(description: String) {
    println(description)
    println()
    println("You won!")
}
