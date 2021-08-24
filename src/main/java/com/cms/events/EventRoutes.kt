package com.cms.events

import akka.actor.ActorRef
import akka.http.javadsl.marshallers.jackson.Jackson
import akka.http.javadsl.model.ContentTypes
import akka.http.javadsl.model.HttpEntity
import akka.http.javadsl.model.HttpHeader
import akka.http.javadsl.model.HttpResponse
import akka.http.javadsl.model.StatusCodes
import akka.http.javadsl.server.AllDirectives
import akka.http.javadsl.server.PathMatchers
import akka.http.javadsl.server.Route
import akka.http.javadsl.unmarshalling.Unmarshaller
import akka.pattern.Patterns.ask
import com.cms.events.messages.CreateEventClashesResult
import com.cms.events.messages.CreateEventCommand
import com.cms.events.messages.CreateEventResult
import com.cms.events.messages.CreateEventSubmittedResult
import com.cms.events.messages.GetAllEventsQuery
import com.cms.events.messages.GetAllEventsQueryResult
import com.cms.events.messages.GetEventsFromPeriodQuery
import com.cms.events.messages.GetEventsFromPeriodResult
import com.cms.events.messages.MessageEnvelope
import com.cms.events.messages.UpdateEventClashesResult
import com.cms.events.messages.UpdateEventCommand
import com.cms.events.messages.UpdateEventNotFoundResult
import com.cms.events.messages.UpdateEventResult
import com.cms.events.messages.UpdateEventSubmittedResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.time.Duration
import java.time.LocalDateTime

internal class EventRoutes(private val eventActorSupervisor: ActorRef) : AllDirectives() {

    private val timeout = Duration.ofSeconds(5)
    private val createEventCommandUnmarshaller: Unmarshaller<HttpEntity, CreateEventCommand> =
        Jackson.unmarshaller(JsonSerializerFactory.jsonSerializer().objectMapper, CreateEventCommand::class.java)
    private val updateEventCommandUnmarshaller: Unmarshaller<HttpEntity, UpdateEventCommand> =
        Jackson.unmarshaller(JsonSerializerFactory.jsonSerializer().objectMapper, UpdateEventCommand::class.java)

    fun createRoutes(): Route {
        return pathPrefix(
            "events"
        ) {
            concat(
                pathPrefix("users") {
                    pathPrefix(
                        PathMatchers.segment()
                    ) { userId ->
                        concat(
                            pathPrefix("events") {
                                concat(
                                    pathEnd {
                                        post { createEvent(userId) }
                                    },
                                    parameter("startDate") { startDate ->
                                        parameter("endDate") { endDate ->
                                            getEventsFromPeriod(
                                                userId,
                                                LocalDateTime.parse(startDate),
                                                LocalDateTime.parse(endDate)
                                            )
                                        }
                                    },
                                    pathEnd {
                                        get { getEvents(userId) }
                                    },
                                    pathPrefix(
                                        PathMatchers.segment()
                                    ) { eventId ->
                                        pathEnd {
                                            put { updateEvent(userId, eventId) }
                                        }
                                    }
                                )
                            })
                    }
                }
            )
        }
    }

    private fun getEvents(userId: String): Route {
        val query = GetAllEventsQuery()
        val message = MessageEnvelope(query, userId)
        val result = ask(eventActorSupervisor, message, timeout).thenApply { it as GetAllEventsQueryResult }
            .thenApply { OBJECT_MAPPER.writeValueAsString(it) }
            .thenApply { ok(it) }
            .thenApply { complete(it) }
        return onComplete(result) { it.get() }
    }

    private fun createEvent(userId: String): Route {
        return entity(createEventCommandUnmarshaller) { entity ->
            val result = ask(eventActorSupervisor, MessageEnvelope(entity, userId), timeout)
                .thenApply { it as CreateEventResult }
                .thenApply {
                    when (it) {
                        is CreateEventClashesResult -> badRequest("Event clashes with other events")
                        is CreateEventSubmittedResult -> created(it.eventId)
                    }
                }.thenApply { complete(it) }
            onComplete(result) { it.get() }
        }
    }

    private fun updateEvent(userId: String, eventId: String): Route {
        return entity(updateEventCommandUnmarshaller) { entity ->
            val result = ask(eventActorSupervisor, MessageEnvelope(entity, userId), timeout)
                .thenApply { it as UpdateEventResult }
                .thenApply {
                    when (it) {
                        is UpdateEventNotFoundResult -> badRequest("Event $eventId does not exist")
                        is UpdateEventClashesResult -> badRequest("Event clashes with other events")
                        is UpdateEventSubmittedResult -> ok(it.eventId)
                    }
                }.thenApply { complete(it) }
            onComplete(result) { it.get() }
        }
    }

    private fun getEventsFromPeriod(userId: String, startDate: LocalDateTime, endDate: LocalDateTime): Route {
        val query = GetEventsFromPeriodQuery(startDate, endDate)
        val message = MessageEnvelope(query, userId)
        val result = ask(eventActorSupervisor, message, timeout).thenApply { it as GetEventsFromPeriodResult }
            .thenApply { OBJECT_MAPPER.writeValueAsString(it) }
            .thenApply { ok(it) }
            .thenApply { complete(it) }
        return onComplete(result) { it.get() }
    }

    private fun badRequest(message: String): HttpResponse {
        return HttpResponse.create()
            .withStatus(StatusCodes.BAD_REQUEST)
            .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
            .addHeader(HttpHeader.parse("Content-Type", "application/json"))
            .withEntity(ContentTypes.APPLICATION_JSON, message)
    }

    private fun created(message: String): HttpResponse {
        return HttpResponse.create()
            .withStatus(StatusCodes.CREATED)
            .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
            .addHeader(HttpHeader.parse("Content-Type", "application/json"))
            .withEntity(ContentTypes.APPLICATION_JSON, message)
    }

    private fun ok(message: String): HttpResponse {
        return HttpResponse.create()
            .withStatus(StatusCodes.OK)
            .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
            .addHeader(HttpHeader.parse("Content-Type", "application/json"))
            .withEntity(ContentTypes.APPLICATION_JSON, message)
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