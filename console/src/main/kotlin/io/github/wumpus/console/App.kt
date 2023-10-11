package io.github.wumpus.console

private fun readShouldContinue(): Boolean {
    while (true) {
        print("Play again (Y/N)? ")
        val input = readln()
        if (input.equals("y", ignoreCase = true)) {
            return true
        }
        if (input.equals("n", ignoreCase = true)) {
            return false
        }
    }
}

fun main() {
    do {
        play()
    } while (readShouldContinue())
}
