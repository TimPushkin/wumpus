package io.github.wumpus.tgbot.data

import dev.inmo.tgbotapi.types.MessageId
import io.github.wumpus.core.Game

data class UserData(var score: Int = 0, var gameState: GameState? = null)

data class GameState(val game: Game, var latestTurnMessageId: MessageId, var status: TurnStatus)

enum class TurnStatus { WAITING, AIMING }
