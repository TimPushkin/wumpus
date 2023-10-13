package io.github.wumpus.tgbot

import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.getMyCommands
import dev.inmo.tgbotapi.extensions.api.bot.getMyDescription
import dev.inmo.tgbotapi.extensions.api.bot.getMyName
import dev.inmo.tgbotapi.extensions.api.bot.getMyShortDescription
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.bot.setMyDescription
import dev.inmo.tgbotapi.extensions.api.bot.setMyName
import dev.inmo.tgbotapi.extensions.api.bot.setMyShortDescription
import dev.inmo.tgbotapi.extensions.api.chat.get.getChat
import dev.inmo.tgbotapi.extensions.api.edit.text.editMessageText
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.api.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.chat.UsernameChat
import dev.inmo.tgbotapi.types.message.HTMLParseMode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.wumpus.configs.generateClassicGame
import io.github.wumpus.core.Game
import io.github.wumpus.core.MovementOutcome
import io.github.wumpus.core.ShootingOutcome
import io.github.wumpus.tgbot.chat.BotProfile
import io.github.wumpus.tgbot.chat.Narration
import io.github.wumpus.tgbot.controls.CallbackData
import io.github.wumpus.tgbot.controls.Commands
import io.github.wumpus.tgbot.controls.Keyboards
import io.github.wumpus.tgbot.controls.Texts
import io.github.wumpus.tgbot.data.GameState
import io.github.wumpus.tgbot.data.TurnStatus
import io.github.wumpus.tgbot.data.UserDataStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.exists

private val LOG = KotlinLogging.logger { }

class Bot(token: String, private val userDataPath: Path) : AutoCloseable {
    private val bot = telegramBot(token)
    private val users = if (userDataPath.exists()) UserDataStorage.fromFile(userDataPath) else UserDataStorage()

    suspend fun start(): Job {
        warnIfCanJoinGroups()
        updateConfiguration()
        return bot.buildBehaviourWithLongPolling(
            defaultExceptionsHandler = { if (it !is CancellationException) LOG.error(it) { "Error occurred" } }
        ) {
            onCommand(Commands.START.command) { sendInstructions(it.chat) }
            onCommand(Commands.HELP.command) { sendInstructions(it.chat) }
            onCommand(Commands.SCORES.command) { sendScores(it.chat) }

            with(Texts) {
                onText({ isPlayText(it.content.text) && users[it.chat.id]?.gameState == null }) {
                    startGame(it.chat)
                }
                onText({ isAimText(it.content.text) && users[it.chat.id]?.gameState?.status == TurnStatus.AIMING }) {
                    shoot(it.chat, aimTextToRoomIds(it.content.text))
                }
            }

            with(CallbackData) {
                onMessageDataCallbackQuery({ isMoveData(it.data) }) { move(it.message.chat, moveDataToRoomId(it.data)) }
                onMessageDataCallbackQuery(START_AIMING) { startAiming(it.message.chat) }
                onMessageDataCallbackQuery(STOP_AIMING) { stopAiming(it.message.chat) }
                onMessageDataCallbackQuery(QUIT) { quitGame(it.message.chat) }
            }

            LOG.info { "Started" }
        }
    }

    private suspend fun warnIfCanJoinGroups() {
        val botInfo = bot.getMe()
        if (botInfo.canJoinGroups) {
            LOG.warn {
                "This bot is designed for private chats, but its can_join_groups is set — disable this via BotFather"
            }
        }
    }

    private suspend fun updateConfiguration() {
        if (bot.getMyName().name != BotProfile.NAME) {
            check(bot.setMyName(BotProfile.NAME))
            LOG.info { "Updated name" }
        }

        if (bot.getMyShortDescription().shortDescription != BotProfile.SHORT_DESCRIPTION) {
            check(bot.setMyShortDescription(BotProfile.SHORT_DESCRIPTION))
            LOG.info { "Updated short description" }
        }

        if (bot.getMyDescription().description != BotProfile.DESCRIPTION) {
            check(bot.setMyDescription(BotProfile.DESCRIPTION))
            LOG.info { "Updated description" }
        }

        val commands = Commands.entries.map { BotCommand(it.command, it.description) }
        if (bot.getMyCommands() != commands) {
            check(bot.setMyCommands(commands))
            LOG.info { "Updated commands" }
        }
    }

    private suspend fun BehaviourContext.sendInstructions(chat: Chat) {
        LOG.debug { "${chat.id}: sending instructions" }
        users.tryWithLockedData(chat.id) {
            sendTextMessage(
                chat,
                Narration.INSTRUCTIONS,
                HTMLParseMode,
                disableWebPagePreview = true,
                replyMarkup = Keyboards.PLAY_REPLY_KEYBOARD.takeIf { gameState == null }
            )
        }
    }

    private suspend fun BehaviourContext.sendScores(chat: Chat, topN: Int = 10) {
        LOG.debug { "${chat.id}: sending scores" }
        users.tryWithLockedData(chat.id) {
            val leaderboard = mutableListOf<Pair<String, Int>>().apply {
                for ((chatId, score) in users.getScores().sortedByDescending { (_, score) -> score }) {
                    if (size == topN) break
                    if (score == 0) continue
                    (getChat(chatId) as? UsernameChat)?.username?.run { add(username.removePrefix("@") to score) }
                }
            }
            sendTextMessage(chat, Narration.narrateLeaderboard(score, leaderboard), HTMLParseMode)
        }
    }

    private suspend fun BehaviourContext.startGame(chat: Chat) {
        LOG.debug { "${chat.id}: starting a game" }
        users.tryWithLockedData(chat.id) {
            if (gameState != null) {
                LOG.debug { "${chat.id}: game already started" }
                return@tryWithLockedData
            }

            // This must be a separate message to remove the play keyboard
            sendTextMessage(
                chat,
                Narration.BEGIN_HUNT,
                parseMode = HTMLParseMode,
                replyMarkup = ReplyKeyboardRemove()
            )

            val game = generateClassicGame()
            val turnMessageId = sendTurnMessage(chat, game)
            gameState = GameState(game, turnMessageId, TurnStatus.WAITING)
        }
    }

    private suspend fun BehaviourContext.sendTurnMessage(chat: Chat, game: Game): MessageId {
        LOG.debug { "${chat.id}: sending a turn message" }
        return sendTextMessage(
            chat,
            Narration.narrateTurnDecision(game),
            parseMode = HTMLParseMode,
            replyMarkup = Keyboards.createTurnInlineKeyboard(game.getHunterRoom().neighborIds)
        ).messageId
    }

    private suspend fun BehaviourContext.move(chat: Chat, roomId: Int) {
        LOG.debug { "${chat.id}: moving to $roomId" }
        users.tryWithLockedData(chat.id) {
            val gameState = gameState
            if (gameState?.status != TurnStatus.WAITING) {
                LOG.debug { "${chat.id}: cannot move with turn status ${gameState?.status}" }
                return@tryWithLockedData
            }

            if (roomId !in gameState.game.getHunterRoom().neighborIds) {
                LOG.debug { "${chat.id}: cannot move to $roomId not in ${gameState.game.getHunterRoom().neighborIds}" }
                return@tryWithLockedData
            }

            editMessageText(
                chat,
                gameState.latestTurnMessageId,
                Narration.narrateTurnMovement(gameState.game, roomId),
                parseMode = HTMLParseMode
            )

            val outcomes = gameState.game.move(roomId)
            val outcomesDescriptionText = Narration.narrateMovementOutcomes(outcomes)
            if (outcomesDescriptionText.isNotEmpty()) {
                sendTextMessage(chat, outcomesDescriptionText)
            }

            if (outcomes.last() == MovementOutcome.ENTERED) {
                gameState.latestTurnMessageId = sendTurnMessage(chat, gameState.game)
            } else {
                LOG.debug { "${chat.id}: game lost after moving" }
                sendTextMessage(
                    chat,
                    Narration.YOU_LOST,
                    parseMode = HTMLParseMode,
                    replyMarkup = Keyboards.PLAY_REPLY_KEYBOARD
                )
                this.gameState = null
            }
        }
    }

    private suspend fun BehaviourContext.startAiming(chat: Chat) {
        LOG.debug { "${chat.id}: starting aiming" }
        users.tryWithLockedData(chat.id) {
            val gameState = gameState
            if (gameState?.status != TurnStatus.WAITING) {
                LOG.debug { "${chat.id}: cannot aim with turn status ${gameState?.status}" }
                return@tryWithLockedData
            }

            editMessageText(
                chat,
                gameState.latestTurnMessageId,
                Narration.narrateTurnAiming(gameState.game),
                parseMode = HTMLParseMode,
                replyMarkup = Keyboards.STOP_AIMING_INLINE_KEYBOARD
            )

            gameState.status = TurnStatus.AIMING
        }
    }

    private suspend fun BehaviourContext.stopAiming(chat: Chat) {
        LOG.debug { "${chat.id}: stopping aiming" }
        users.tryWithLockedData(chat.id) {
            val gameState = gameState
            if (gameState?.status != TurnStatus.AIMING) {
                LOG.debug { "${chat.id}: cannot stop aiming with turn status ${gameState?.status}" }
                return@tryWithLockedData
            }

            editMessageText(
                chat,
                gameState.latestTurnMessageId,
                Narration.narrateTurnDecision(gameState.game),
                parseMode = HTMLParseMode,
                replyMarkup = Keyboards.createTurnInlineKeyboard(gameState.game.getHunterRoom().neighborIds)
            )

            gameState.status = TurnStatus.WAITING
        }
    }

    private suspend fun BehaviourContext.shoot(chat: Chat, roomIds: Collection<Int>) {
        LOG.debug { "${chat.id}: shooting through rooms ${roomIds.joinToString(", ")}" }
        users.tryWithLockedData(chat.id) {
            val gameState = gameState
            if (gameState?.status != TurnStatus.AIMING) {
                LOG.debug { "${chat.id}: cannot shoot with turn status ${gameState?.status}" }
                return@tryWithLockedData
            }

            check(roomIds.isNotEmpty())
            if (roomIds.size > gameState.game.maxArrowFlightRooms) {
                LOG.debug { "${chat.id}: too many rooms for shooting specified" }
                return@tryWithLockedData
            }

            editMessageText(
                chat,
                gameState.latestTurnMessageId,
                Narration.narrateTurnShooting(gameState.game, roomIds),
                parseMode = HTMLParseMode
            )

            val outcome = gameState.game.shoot(roomIds)

            sendTextMessage(chat, Narration.narrateShootingOutcome(outcome, gameState.game.getArrowsLeft() > 0))

            if (outcome == ShootingOutcome.MISSED && gameState.game.getArrowsLeft() > 0) {
                gameState.latestTurnMessageId = sendTurnMessage(chat, gameState.game)
                gameState.status = TurnStatus.WAITING
            } else if (outcome == ShootingOutcome.DEFEATED_WUMPUS) {
                LOG.debug { "${chat.id}: game won" }
                sendTextMessage(
                    chat,
                    Narration.narrateCongratulations(score + 1),
                    parseMode = HTMLParseMode,
                    replyMarkup = Keyboards.PLAY_REPLY_KEYBOARD
                )
                this.gameState = null
                score += 1
            } else {
                LOG.debug { "${chat.id}: game lost after shooting" }
                sendTextMessage(
                    chat,
                    Narration.YOU_LOST,
                    parseMode = HTMLParseMode,
                    replyMarkup = Keyboards.PLAY_REPLY_KEYBOARD
                )
                this.gameState = null
            }
        }
    }

    private suspend fun BehaviourContext.quitGame(chat: Chat) {
        LOG.debug { "${chat.id}: quitting" }
        users.tryWithLockedData(chat.id) {
            gameState?.apply {
                editMessageText(chat, latestTurnMessageId, Narration.narrateTurnQuit(game), parseMode = HTMLParseMode)
                // This must be a separate message to create the play keyboard
                sendMessage(chat, Narration.WUMPUS_REMAINS, replyMarkup = Keyboards.PLAY_REPLY_KEYBOARD)
            } ?: LOG.debug { "${chat.id}: no game started" }
            gameState = null
        }
    }

    override fun close() {
        LOG.debug { "Finishing" }
        runBlocking {
            users.withFullLock("closing") {
                bot.close()
                users.saveTo(userDataPath, doLocked = false)
            }
        }
        LOG.info { "Finished" }
    }
}
