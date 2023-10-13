package io.github.wumpus.tgbot.controls

enum class Commands(val command: String, val description: String) {
    START("start", "send start message"),
    HELP("help", "send instructions"),
    SCORES("scores", "show the leaderboard")
}

object Texts {
    const val PLAY = "▶ Play!"

    private val AIM_REGEX = Regex("""(\d+ +)*\d+""")

    fun isPlayText(text: String) = text == PLAY
    fun isAimText(text: String) = text.matches(AIM_REGEX)

    fun aimTextToRoomIds(text: String) = text.split(' ').map { it.toInt() }
}

object CallbackData {
    const val MOVE_PREFIX = "move_to_"
    const val START_AIMING = "start_aiming"
    const val STOP_AIMING = "stop_aiming"
    const val QUIT = "quit"

    private val MOVE_TO_REGEX = Regex("""$MOVE_PREFIX\d+""")

    fun isMoveData(data: String) = data.matches(MOVE_TO_REGEX)

    fun moveDataToRoomId(data: String) = data.substring(MOVE_PREFIX.length).toInt()
}
