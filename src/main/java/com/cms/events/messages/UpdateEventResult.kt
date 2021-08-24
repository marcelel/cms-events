package com.cms.events.messages

import com.cms.events.SerializableMessage

sealed class UpdateEventResult : SerializableMessage

data class UpdateEventNotFoundResult(val userId: String, val eventId: String) : UpdateEventResult()

data class UpdateEventClashesResult(val userId: String, val eventId: String) : UpdateEventResult()

data class UpdateEventSubmittedResult(val userId: String, val eventId: String) : UpdateEventResult()