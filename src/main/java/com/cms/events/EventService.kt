package com.cms.events

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder

class EventService private constructor(private val eventRepository: EventRepository) : AbstractActor() {

    companion object {

        @JvmStatic
        fun create(eventRepository: EventRepository): Props {
            return Props.create(EventService::class.java) { EventService(eventRepository) }
        }
    }

    override fun createReceive(): Receive {
        return ReceiveBuilder.create()
            .match(Message::class.java, this::handle)
            .build()
    }

    data class Message(val to: String, val eventMessage: EventMessage)

    private fun handle(message: Message) {
        val loyaltyActor = context
            .child(message.to)
            .getOrElse { createLoyaltyActor(message.to) }
        loyaltyActor.forward(message.eventMessage, context)
    }

    private fun createLoyaltyActor(id: String): ActorRef {
        return context.actorOf(EventActor.create(eventRepository), id)
    }
}