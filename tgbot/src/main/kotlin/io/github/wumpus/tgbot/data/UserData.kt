package io.github.wumpus.tgbot.data

import dev.inmo.tgbotapi.types.MessageId
import io.github.wumpus.core.Game
import kotlinx.serialization.Serializable

@Serializable
data class UserData(var score: Int = 0, var gameState: GameState? = null)

@Serializable
data class GameState(val game: Game, var latestTurnMessageId: MessageId, var status: TurnStatus)

enum class TurnStatus { WAITING, AIMING }
