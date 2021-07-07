package com.cms.events

import akka.actor.ActorRef
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers
import akka.http.javadsl.server.Route
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.pattern.Patterns.ask
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.time.Duration

internal class EventRoutes(private val eventService: ActorRef) : AllDirectives() {

    private val timeout = Duration.ofSeconds(5)
    private val createEventCommandUnmarshaller: Unmarshaller<HttpEntity, CreateEventCommand> =
        Jackson.unmarshaller(JsonSerializerFactory.jsonSerializer().objectMapper, CreateEventCommand::class.java)

    fun createRoutes(): Route {
        return pathPrefix(
            "events"
        ) {
            concat(
                pathPrefix(
                    PathMatchers.segment()
                ) { id: String ->
                    get {
                        getEvent(id)
                    }
                },
                pathEnd { post { createEvent() } },
                pathEnd { get { getEvents() } }
            )
        }
    }

    private fun getEvents(): Route {
        return try {
            complete(
                HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
//                    .withEntity(OBJECT_MAPPER.writeValueAsString(events.values))
            )
        } catch (e: JsonProcessingException) {
            complete("[]")
        }
    }

    private fun getEvent(id: String): Route {
        val query = GetEventQuery()
        val message = EventService.Message(id, query)
        val result = ask(eventService, message, timeout).thenApply { it as GetEventQueryResult }
            .thenApply { it.event }
            .thenApply { if (it != null) OBJECT_MAPPER.writeValueAsString(it) else "" }
            .thenApply {
                if (it.isEmpty()) {
                    HttpResponse.create()
                        .withStatus(StatusCodes.BAD_REQUEST)
                        .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                        .withEntity("Event with id $id does not exist")
                } else {
                    HttpResponse.create()
                        .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                        .withEntity(it)
                }
            }.thenApply { complete(it) }
        return onComplete(result) { it.get() }
    }

    private fun createEvent(): Route {
        return entity(createEventCommandUnmarshaller) {
            eventService.tell(EventService.Message(eventMessage = it), ActorRef.noSender())
            complete(
                HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                    .withStatus(StatusCodes.CREATED)
            )
        }
    }

    companion object {

        val OBJECT_MAPPER = objectMapper
        private val objectMapper: ObjectMapper
            get() {
                val mapper = ObjectMapper()
                mapper.registerModule(JavaTimeModule())
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                return mapper
            }
    }
}