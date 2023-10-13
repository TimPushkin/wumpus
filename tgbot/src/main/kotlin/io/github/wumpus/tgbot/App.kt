package io.github.wumpus.tgbot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path

private fun registerShutdownHook(bot: Bot) {
    Runtime.getRuntime().addShutdownHook(object : Thread("shutdown-hook") {
        override fun run() = bot.close()
    })
}

class Launcher : CliktCommand() {
    private val token by option(
        help = "Telegram bot token to use (env var TOKEN must be used otherwise)",
        envvar = "TOKEN"
    )
    private val dataPath by option(help = "Path to user data (will create, if absent)")
        .path(canBeDir = false, mustBeReadable = true, mustBeWritable = true)
        .default(Path("user_data.json"))

    override fun run() {
        val bot = Bot(requireNotNull(token) { "Bot token not provided" }, dataPath)
        registerShutdownHook(bot)
        println("Starting the bot. Press Ctrl-C to exit.")
        runBlocking { bot.start().join() }
    }
}

fun main(args: Array<String>) = Launcher().main(args)
