package com.cms.events;

import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static akka.http.javadsl.server.PathMatchers.integerSegment;

class EventRoutes extends AllDirectives {

    public static final ObjectMapper OBJECT_MAPPER = getObjectMapper();

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private final Duration timeout = Duration.ofSeconds(5);
    private final Map<Integer, Event> events = Map.of(
            1, new Event(1, "Daily Meeting", "Daily meeting very long description",
                    LocalDateTime.now(), LocalDateTime.now().plusMinutes(30), "John",
                    List.of("John", "Tom", "Kate", "Jane"), comments),
            2, new Event(2, "Backlog Grooming", "Backlog Grooming very long description",
                    LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), "Kate",
                    List.of("John", "Tom", "Kate", "Jane"), comments),
            3, new Event(3, "Spring planning", "Spring planning very long description",
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusHours(2), "Jane",
                    List.of("John", "Tom", "Kate", "Jane"), comments)
            );

    Route createRoutes() {
        return pathPrefix("events", () ->
            concat(
                pathPrefix(integerSegment(), id ->
                    get(() -> getEvent(id))),
                pathEnd(() ->
                    get(this::getEvents))
            )
        );
    }

    private Route getEvents() {
        try {
            return complete(HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                    .withEntity(OBJECT_MAPPER.writeValueAsString(events.values())));
        } catch (JsonProcessingException e) {
            return complete("[]");
        }
    }

    private Route getEvent(Integer id) {
        try {
            return complete(HttpResponse.create()
                    .addHeader(HttpHeader.parse("Access-Control-Allow-Origin", "*"))
                    .withEntity(OBJECT_MAPPER.writeValueAsString(events.get(id))));
        } catch (JsonProcessingException e) {
            return complete("[]");
        }
    }
}
