package com.cms.events

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.actor.Props
import akka.pattern.Patterns.pipe
import com.cms.events.messages.CreateEventClashesResult
import com.cms.events.messages.CreateEventCommand
import com.cms.events.messages.CreateEventSubmittedResult
import com.cms.events.messages.GetAllEventsQuery
import com.cms.events.messages.GetAllEventsQueryResult
import com.cms.events.messages.GetEventsFromPeriodQuery
import com.cms.events.messages.GetEventsFromPeriodResult
import com.cms.events.messages.UpdateEventClashesResult
import com.cms.events.messages.UpdateEventCommand
import com.cms.events.messages.UpdateEventNotFoundResult
import com.cms.events.messages.UpdateEventSubmittedResult
import com.cms.events.utils.format
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

class UserActor private constructor(
    private val activityService: ActivityService,
    private val eventRepository: EventRepository
) : AbstractActorWithStash() {

    private val userId = self.path().name()
    private var events: MutableList<Event> = mutableListOf()

    companion object {

        @JvmStatic
        fun create(
            activityService: ActivityService,
            eventRepository: EventRepository
        ): Props {
            return Props.create(UserActor::class.java) {
                UserActor(activityService, eventRepository)
            }
        }
    }

    override fun preStart() {
        super.preStart()
        val initialized = eventRepository.findByUserId(userId)
            .thenApply { Initialized(it) }
        pipe(initialized, context.dispatcher).to(self)
    }

    override fun createReceive(): Receive {
        return initializing()
    }

    private fun initializing(): Receive {
        return receiveBuilder()
            .match(
                Initialized::class.java
            ) {
                events = it.events.toMutableList()
                context.become(running())
                unstashAll()
            }
            .matchAny { stash() }
            .build()
    }

    private fun running(): Receive {
        return receiveBuilder()
            .match(CreateEventCommand::class.java, this::handle)
            .match(UpdateEventCommand::class.java, this::handle)
            .match(GetEventsFromPeriodQuery::class.java, this::handle)
            .match(GetAllEventsQuery::class.java, this::handle)
            .build()
    }

    private fun handle(eventCommand: CreateEventCommand) {
        val event = eventCommand.toEvent(UUID.randomUUID().toString())
        if (doesAnyEventClash(event, events)) {
            sender.tell(CreateEventClashesResult(userId), self)
            return
        }

        events.add(event)
        val result = eventRepository.save(event)
            .thenCompose { publishActivity("New event ${event.title} created from ${event.startDate.format()} to ${event.endDate.format()}") }
            .thenApply { CreateEventSubmittedResult(userId, event._id) }
        pipe(result, context.dispatcher).to(sender)
    }

    private fun handle(command: UpdateEventCommand) {
        val event = events.find { it._id == command._id }

        if (event == null) {
            sender.tell(UpdateEventNotFoundResult(userId, command._id), self)
            return
        }

        if (doesAnyEventClash(event, events.filter { it._id != event._id })) {
            sender.tell(UpdateEventClashesResult(userId, event._id), self)
            return
        }

        val updatedEvent = command.toEvent()
        val index = events.indexOf(event)
        events.set(index, updatedEvent)
        val result = eventRepository.update(updatedEvent)
            .thenCompose { publishActivity("Event with id ${event._id} and title ${event.title} updated") }
            .thenApply { UpdateEventSubmittedResult(userId, event._id) }
        pipe(result, context.dispatcher).to(sender)
    }

    private fun handle(query: GetEventsFromPeriodQuery) {
        val eventsFromPeriod = events.filter {
            it.endDate.isAfter(query.startDate) && it.startDate.isBefore(query.endDate)
        }
        val result = GetEventsFromPeriodResult(eventsFromPeriod)
        sender.tell(result, self)
    }

    private fun handle(ignored: GetAllEventsQuery) {
        sender.tell(GetAllEventsQueryResult(events), self)
    }

    private fun doesAnyEventClash(newEvent: Event, events: List<Event>): Boolean {
        return events.any {
            it.endDate.isAfter(newEvent.startDate) && it.startDate.isBefore(newEvent.endDate)
        }
    }

    private fun publishActivity(message: String): CompletableFuture<Done> {
        val activity = Activity(
            userId,
            LocalDateTime.now(),
            message
        )
        return activityService.publish(activity)
    }

    private data class Initialized(val events: List<Event>)
}