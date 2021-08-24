package com.cms.events.messages

import com.cms.events.Event
import com.cms.events.EventType
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import java.time.LocalDateTime

sealed class EventMessage

class GetEventQuery : EventMessage()

class GetAllEventsQuery : EventMessage()

data class GetEventQueryResult(val event: Event?)

data class GetAllEventsQueryResult(val events: List<Event>)

data class GetEventsFromPeriodQuery(val startDate: LocalDateTime, val endDate: LocalDateTime) : EventMessage()

data class CreateEventCommand(
    val title: String,
    val type: EventType,
    val description: String,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val startDate: LocalDateTime,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val endDate: LocalDateTime,
    val author: String,
    val users: List<String> = emptyList()
) : EventMessage() {

    fun toEvent(id: String): Event {
        return Event(id, title, type, description, startDate, endDate, author, users, emptyList())
    }
}

data class UpdateEventCommand(
    val _id: String,
    val title: String,
    val type: EventType,
    val description: String,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val startDate: LocalDateTime,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val endDate: LocalDateTime,
    val author: String,
    val users: List<String> = emptyList()
) : EventMessage() {

    fun toEvent(): Event {
        return Event(_id, title, type, description, startDate, endDate, author, users, emptyList())
    }
}

data class DeleteEventCommand(val eventId: String) : EventMessage()