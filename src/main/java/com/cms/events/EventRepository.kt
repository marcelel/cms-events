package com.cms.events

import akka.Done
import com.cms.events.mongo.ReadDataStore
import java.util.concurrent.CompletableFuture

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
}