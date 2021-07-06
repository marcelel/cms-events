package com.cms.events.mongo

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mongodb.javadsl.MongoSink
import akka.stream.alpakka.mongodb.javadsl.MongoSource
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import com.cms.events.Event
import com.cms.events.JsonSerializerFactory
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.Document
import java.util.concurrent.CompletableFuture

class ReadDataStore(private val system: ActorSystem, private val mongoDatabase: MongoDatabase) {

    private val ID_FIELD = "_id"
    private val objectMapper = JsonSerializerFactory.jsonSerializer().objectMapper

    fun find(id: String): CompletableFuture<Event?> {
        val collection = mongoDatabase.getCollection("events")
        val publisher = collection.find(Document().append(ID_FIELD, id)).first()
        val source = MongoSource.create(publisher)
        return source.runWith(Sink.seq(), system)
            .toCompletableFuture()
            .thenApply { if (it.size > 0) it.first() else null }
            .thenApply { if (it != null) objectMapper.convertValue(it, Event::class.java) else null }
    }

    fun save(event: Event): CompletableFuture<Done> {
        val collection = mongoDatabase.getCollection("events")
        val json = objectMapper.writeValueAsString(event)
        val document = Document.parse(json)
        return Source.single(document).runWith(MongoSink.insertOne(collection), system)
            .toCompletableFuture()
    }
}