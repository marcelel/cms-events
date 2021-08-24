package com.cms.events.messages;

import com.cms.events.SerializableMessage;

import java.util.UUID;

public class MessageEnvelope implements SerializableMessage {

    private final EventMessage message;
    private final String to;

    public EventMessage getMessage() {
        return message;
    }

    public String getTo() {
        return to;
    }

    public MessageEnvelope(EventMessage message) {
        this(message, UUID.randomUUID().toString());
    }

    public MessageEnvelope(EventMessage message, String to) {
        this.message = message;
        this.to = to;
    }
}
