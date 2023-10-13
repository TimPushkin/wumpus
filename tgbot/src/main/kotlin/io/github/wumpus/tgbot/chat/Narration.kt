package io.github.wumpus.tgbot.chat

import io.github.wumpus.core.Game
import io.github.wumpus.core.MovementOutcome
import io.github.wumpus.core.ShootingOutcome

object Narration {
    val INSTRUCTIONS = """
        <b>Welcome to "Hunt the Wumpus"!</b>
        Inspired by the original 1973 version by Gregory Yob.
        
        The Wumpus lives in a cave forming a <a href="https://en.wikipedia.org/wiki/Regular_dodecahedron">dodecahedron</a>: 20 rooms, each having 3 tunnels leading to other rooms.
        
        The rooms may contain hazards:
        1. Two rooms have <i>bottomless pits</i> — you lose if you walk into one.
        2. Two other rooms have <i>super bats</i>. If you go there, a bat grabs you and takes you to some random room (possibly a dangerous one).
        
        The Wumpus is not bothered by the hazards: it has suckers helping from falling and is heavy to not be lifted.
        
        On each turn you can either:
        1. <i>Move</i> to a neighboring room. The Wumpus is sleeping and your steps will not wake it unless you wonder into its room, in which case it eats you (and you lose).
        2. <i>Shoot</i> one of 5 crooked arrows (you lose if you run out). When shooting, you can pick 1–5 rooms consecutively reachable through the tunnels (or the arrow will fly at random). If you hit the Wumpus you win, otherwise it may move one room at random (p = 0.75). Be careful not to shoot yourself (or you lose)!
        
        When you are in a room next to the Wumpus or a hazard you will feel it:
        - There is a <i>draft</i> from a pit
        - Bets can be <i>heard</i>
        - The Wumpus has a distinct <i>smell</i>
        """.trimIndent()
    const val BEGIN_HUNT = "⚔ <b>Let the hunt begin!</b>"
    const val YOU_LOST = "☠ <b>You lost!</b> Better luck next time!"
    const val WUMPUS_REMAINS = "\uD83D\uDE1E The Wumpus remains undefeated."

    fun narrateLeaderboard(userScore: Int, leaderboard: Iterable<Pair<String, Int>>) =
        "<b>Your score is $userScore</b> — a point is awarded for defeating the Wumpus.\n\n" +
                "<b>Leaderboard:</b>\n" + leaderboard.withIndex().joinToString("\n") { (i, p) ->
            p.let { (username, score) -> "${i + 1}. $username — $score." }
        }

    private fun narrateTurn(game: Game, createEnding: StringBuilder.() -> Unit) = buildString {
        with(game.getHunterRoom()) {
            appendLine("\uD83E\uDDED <b>You are in room $id.</b>")
            appendLine("Tunnels lead to rooms: ${neighborIds.joinToString(", ")}.")
        }

        with(game.getHunterSurroundings()) {
            if (haveWumpus || havePits || haveBats) appendLine()
            if (haveWumpus) appendLine("You smell the Wumpus!")
            if (havePits) appendLine("You feel a draft.")
            if (haveBats) appendLine("You hear bats nearby.")
        }

        appendLine()
        createEnding()
    }

    fun narrateTurnDecision(game: Game) = narrateTurn(game) { appendLine("Move or shoot?") }

    fun narrateTurnMovement(game: Game, roomId: Int) = narrateTurn(game) { appendLine("Moved to room $roomId.") }

    fun narrateTurnAiming(game: Game) = narrateTurn(game) {
        appendLine("You are aiming!")
        appendLine("<i>Send a message with 1–${game.maxArrowFlightRooms} space-separated room numbers.</i>")
    }

    fun narrateTurnShooting(game: Game, roomIds: Collection<Int>) = narrateTurn(game) {
        appendLine(
            "Aimed to shoot through room${if (roomIds.size > 1) "s" else ""} ${roomIds.joinToString(", ")}."
        )
    }

    fun narrateTurnQuit(game: Game) = narrateTurn(game) { appendLine("You quit the hunt.") }

    fun narrateMovementOutcomes(outcomes: List<MovementOutcome>) = buildString {
        for (outcome in outcomes) {
            when (outcome) {
                MovementOutcome.SNATCHED_BY_BATS -> appendLine("\uD83E\uDD87 Super bats snatch you to another room!")
                MovementOutcome.ENTERED -> continue
                MovementOutcome.FALLEN_INTO_PIT -> appendLine("\uD83D\uDD73 AAAaaah... You fell into a pit!")
                MovementOutcome.EATEN_BY_WUMPUS -> appendLine("\uD83C\uDF7D You went right into the Wumpus' room and it ate you!")
            }
        }
    }

    fun narrateShootingOutcome(outcome: ShootingOutcome, hasArrowsLeft: Boolean) = when (outcome) {
        ShootingOutcome.MISSED -> "\uD83D\uDE15 Missed!${if (!hasArrowsLeft) " You are out of arrows!" else ""}"
        ShootingOutcome.GOT_SHOT -> "\uD83D\uDE35 Ouch! You shot yourself!"
        ShootingOutcome.DEFEATED_WUMPUS -> "\uD83D\uDCA5 You shot the Wumpus!"
        ShootingOutcome.EATEN_BY_WUMPUS -> "\uD83C\uDF7D You missed and then the Wumpus found you and ate you!"
    }

    fun narrateCongratulations(userScore: Int) =
        "\uD83C\uDF89 <b>Congratulations</b>, your score is now <b>$userScore</b>!"
}
