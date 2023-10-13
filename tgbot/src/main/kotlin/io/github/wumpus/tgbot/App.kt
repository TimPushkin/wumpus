package io.github.wumpus.tgbot

private fun getToken(args: Array<String>) =
    if (args.isNotEmpty()) {
        require(args.size == 1) { "Too many CLI arguments: expecting a single Telegram bot token" }
        args[0]
    } else {
        requireNotNull(System.getenv("TOKEN")) {
            "Provide a Telegram bot token: either as a CLI argument or as a environment variable named TOKEN"
        }
    }

private fun registerShutdownHook(bot: Bot) {
    Runtime.getRuntime().addShutdownHook(object : Thread("shutdown-hook") {
        override fun run() {
            bot.close()
        }
    })
}

suspend fun main(args: Array<String>) {
    val bot = Bot(getToken(args))
    registerShutdownHook(bot)
    println("Starting the bot. Press Ctrl-C to exit.")
    bot.start().join()
}
