package com.cms.events

import akka.Done
import com.cms.events.mongo.ReadDataStore
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

val events = mutableMapOf(
    "1" to Event(
        "1", "Daily Meeting", "Daily meeting very long description",
        LocalDateTime.now(), LocalDateTime.now().plusMinutes(30), "John",
        listOf("John", "Tom", "Kate", "Jane"), listOf(
            Comment("Kate", "I'll be late"),
            Comment("John", "Don't forget about last bug"),
            Comment("Tom", "Need to discuss problem with db")
        )
    ),
    "2" to Event(
        "2", "Backlog Grooming", "Backlog Grooming very long description",
        LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), "Kate",
        listOf("John", "Tom", "Kate", "Jane"), listOf(
            Comment("Kate", "I'll be late"),
            Comment("John", "Don't forget about last bug"),
            Comment("Tom", "Need to discuss problem with db")
        )
    ),
    "3" to Event(
        "3", "Sprint planning", "Sprint planning very long description",
        LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusHours(2), "Jane",
        listOf("John", "Tom", "Kate", "Jane"), listOf(
            Comment("Kate", "I'll be late"),
            Comment("John", "Don't forget about last bug"),
            Comment("Tom", "Need to discuss problem with db")
        )
    )
)

interface EventRepository {

    fun save(event: Event): CompletableFuture<Done>

    fun find(id: String): CompletableFuture<Event?>
}

class MongoEventRepository(private val readDataStore: ReadDataStore) : EventRepository {

    override fun save(event: Event): CompletableFuture<Done> {
        return readDataStore.save(event)
    }

    override fun find(id: String): CompletableFuture<Event?> {
        return readDataStore.find(id)
    }

    fun load() {
        events.values.forEach { readDataStore.save(it).get() }
    }
}

class InMemoryEventRepository(private val executor: Executor) : EventRepository {

    override fun save(event: Event): CompletableFuture<Done> {
        return CompletableFuture.supplyAsync(Supplier {
            events[event._id] = event
            Done.done()
        }, executor)
    }

    override fun find(id: String): CompletableFuture<Event?> {
        return CompletableFuture.supplyAsync(Supplier {
            events[id]
        }, executor)
    }
}