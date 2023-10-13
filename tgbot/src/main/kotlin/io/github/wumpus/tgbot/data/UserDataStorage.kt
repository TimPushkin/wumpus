package io.github.wumpus.tgbot.data

import dev.inmo.tgbotapi.types.IdChatIdentifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class UserDataStorage(data: Map<IdChatIdentifier, UserData> = mutableMapOf()) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromFile(path: Path): UserDataStorage {
            val data = try {
                path.inputStream().use { inp ->
                    Json.decodeFromStream<Map<IdChatIdentifier, UserData>>(inp)
                }
            } catch (e: SerializationException) {
                throw IllegalArgumentException("Cannot create user data storage from $path", e)
            }
            return UserDataStorage(data)
        }
    }

    private val data = data.entries.associateTo(mutableMapOf()) { it.key to (it.value to Mutex()) }
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

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun saveTo(path: Path, doLocked: Boolean = true) {
        val save = {
            val dataSnapshot = data.entries.associate {
                it.value.let { (userData, _) -> it.key to userData }
            }
            path.outputStream().use { out -> Json.encodeToStream(dataSnapshot, out) }
        }
        if (doLocked) {
            withFullLock("saveTo", save)
        } else {
            save()
        }
    }
}
