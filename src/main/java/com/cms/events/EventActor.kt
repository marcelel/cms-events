package com.cms.events

import akka.actor.AbstractActorWithStash
import akka.actor.Props
import akka.pattern.Patterns.pipe

class EventActor private constructor(private val eventRepository: EventRepository) : AbstractActorWithStash() {

    private val eventId = self.path().name()
    private var event: Event? = null

    companion object {

        @JvmStatic
        fun create(eventRepository: EventRepository): Props {
            return Props.create(EventActor::class.java) { EventActor(eventRepository) }
        }
    }

    override fun preStart() {
        super.preStart()
        val initialized = eventRepository.find(eventId).thenApply { Initialized(it) }
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
                event = it.event
                context.become(running())
                unstashAll()
            }
            .matchAny { stash() }
            .build()
    }

    private fun running(): Receive {
        return receiveBuilder()
            .match(GetEventQuery::class.java, this::handle)
            .match(CreateEventCommand::class.java, this::handle)
            .build()
    }

    private fun handle(ignored: GetEventQuery) {
        sender.tell(GetEventQueryResult(event), self)
    }

    private fun handle(eventCommand: CreateEventCommand) {
        val event = eventCommand.toEvent()
        val result = eventRepository.save(event)
        //todo
        pipe(result, context.dispatcher).to(sender)
    }

    private data class Initialized(val event: Event?)
}