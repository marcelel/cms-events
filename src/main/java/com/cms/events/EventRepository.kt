package com.cms.events

import akka.Done
import com.cms.events.mongo.ReadDataStore
import java.util.concurrent.CompletableFuture

interface EventRepository {

    fun save(event: Event): CompletableFuture<Done>

    fun update(user: Event): CompletableFuture<Done>

    fun delete(event: Event): CompletableFuture<Done>

    fun find(id: String): CompletableFuture<Event?>

    fun findByUserId(userId: String): CompletableFuture<List<Event>>
}

class MongoEventRepository(private val readDataStore: ReadDataStore) : EventRepository {

    override fun save(event: Event): CompletableFuture<Done> {
        return readDataStore.save(event)
    }

    override fun update(user: Event): CompletableFuture<Done> {
        return readDataStore.update(user._id, user)
    }

    override fun delete(event: Event): CompletableFuture<Done> {
        return readDataStore.delete(event._id)
    }

    override fun find(id: String): CompletableFuture<Event?> {
        return readDataStore.find(id)
    }

    override fun findByUserId(userId: String): CompletableFuture<List<Event>> {
        return readDataStore.findByUserId(userId)
    }
}