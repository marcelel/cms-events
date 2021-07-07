package com.cms.events

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import java.time.LocalDateTime

sealed class EventMessage

class GetEventQuery : EventMessage()

data class GetEventQueryResult(val event: Event?)

data class CreateEventCommand(
    val title: String,
    val description: String,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val startDate: LocalDateTime,
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val endDate: LocalDateTime,
    val author: String,
    val users: List<String>
) : EventMessage() {
    
    fun toEvent(id: String): Event {
        return Event(id, title, description, startDate, endDate, author, users, emptyList())
    }
}