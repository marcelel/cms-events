package com.cms.events

import java.time.LocalDateTime

sealed class EventMessage

class GetEventQuery : EventMessage()

data class GetEventQueryResult(val event: Event?)

data class CreateEventCommand(
    val title: String,
    val description: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val author: String,
    val users: List<String>
) : EventMessage() {

    fun toEvent(): Event {
        return Event(title, description, startDate, endDate, author, users, emptyList())
    }
}