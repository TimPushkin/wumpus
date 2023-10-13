package io.github.wumpus.tgbot.data

import dev.inmo.tgbotapi.types.IdChatIdentifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UserDataStorage(private val data: MutableMap<IdChatIdentifier, Pair<UserData, Mutex>> = mutableMapOf()) {
    private val globalMutex = Mutex()

    private suspend fun getLockableState(id: IdChatIdentifier) =
        data[id] ?: globalMutex.withLock(id) { data.getOrPut(id) { UserData() to Mutex() } }

    operator fun get(id: IdChatIdentifier) = data[id]?.first

    suspend fun <T> tryWithLockedData(id: IdChatIdentifier, action: suspend UserData.() -> T): T? {
        val (state, mutex) = getLockableState(id)
        // If we decide to use id as the lock owner, we should use `val boxedId: Identifier? = id` instead (otherwise
        // it will be automatically boxed as an Any? argument potentially resulting in referentially-unequal objects in
        // tryLock() and unlock())
        return if (mutex.tryLock("tryWithLockedData")) {
            try {
                state.action()
            } finally {
                mutex.unlock("tryWithLockedData")
            }
        } else {
            null
        }
    }

    fun getScores(): Sequence<Pair<IdChatIdentifier, Int>> = data.asSequence().map { it.key to it.value.first.score }

    suspend fun <T> withFullLock(owner: Any?, action: suspend () -> T): T {
        globalMutex.withLock(owner) {
            data.values.forEach { (_, mutex) -> mutex.lock(owner) }
            try {
                return action()
            } finally {
                data.values.forEach { (_, mutex) -> mutex.unlock(owner) }
            }
        }
    }
}