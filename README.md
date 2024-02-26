# Hunt the Wumpus

A Kotlin implementation of the classic [Hunt the Wumpus](https://en.wikipedia.org/wiki/Hunt_the_Wumpus) game.

## Console game

Type this while in the root of the project to launch the game in the terminal:
```shell
# Option 1: run via Gradle
./gradlew :console:run --console=plain

# Option 2: build a JAR and run it
./gradlew :console:jar
java -jar ./console/build/libs/console.jar
```

...and follow the instructions printed to play!

## Telegram bot

<img src="https://i.ibb.co/6FQ8NJ9/wumpus-tg-bot.gif" alt="Playing via Telegram bot" width=50%>

You need a Telegram bot token to launch the game in this mode. You can obtain one in a couple of minutes by following
[these instructions](https://core.telegram.org/bots/tutorial#obtain-your-bot-token).

After you get a token, you can launch the bot like so:
```shell
# Option 1.1
./gradlew :tgbot:run --console=plain --args="--token=<YOUR_BOT_TOKEN>"

# Option 1.2
./gradlew :tgbot:jar
java -jar ./console/build/libs/console.jar --token="<YOUR_BOT_TOKEN>"

# Option 2.1
export TOKEN="<YOUR_BOT_TOKEN>"
./gradlew :tgbot:run --console=plain

# Option 2.2
export TOKEN="<YOUR_BOT_TOKEN>"
./gradlew :tgbot:jar
java -jar ./console/build/libs/console.jar
```

The bot will save user's session data (game state, score) in a JSON file in the current directory. If there is such file
in the current directory, it will be read when the bot is launched.
