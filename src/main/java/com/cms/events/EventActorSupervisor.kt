package com.cms.events

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import java.util.*

class EventActorSupervisor private constructor(private val eventRepository: EventRepository) : AbstractActor() {

    companion object {

        @JvmStatic
        fun create(eventRepository: EventRepository): Props {
            return Props.create(EventActorSupervisor::class.java) { EventActorSupervisor(eventRepository) }
        }
    }

    override fun createReceive(): Receive {
        return ReceiveBuilder.create()
            .match(Message::class.java, this::handle)
            .build()
    }

    data class Message(val to: String = UUID.randomUUID().toString(), val eventMessage: EventMessage) :
        SerializableMessage

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