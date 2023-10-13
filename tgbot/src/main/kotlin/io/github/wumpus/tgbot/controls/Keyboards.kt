package io.github.wumpus.tgbot.controls

import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatInlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.flatReplyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.buttons.inline.dataInlineButton
import dev.inmo.tgbotapi.utils.row

object Keyboards {
    val PLAY_REPLY_KEYBOARD =
        flatReplyKeyboard(persistent = true, resizeKeyboard = true) { simpleButton(Texts.PLAY) }
    val STOP_AIMING_INLINE_KEYBOARD =
        flatInlineKeyboard { +dataInlineButton("◀ Stop aiming", CallbackData.STOP_AIMING) }

    fun createTurnInlineKeyboard(neighborRoomIds: Set<Int>) = inlineKeyboard {
        row {
            for (roomId in neighborRoomIds) {
                +dataInlineButton("➡ $roomId", "${CallbackData.MOVE_PREFIX}$roomId")
            }
        }
        row {
            +dataInlineButton("\uD83D\uDEAA Quit", CallbackData.QUIT)
            +dataInlineButton("\uD83C\uDFF9 Shoot", CallbackData.START_AIMING)
        }
    }
}
