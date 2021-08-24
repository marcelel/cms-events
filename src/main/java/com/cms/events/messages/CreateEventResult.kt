package com.cms.events.messages

import com.cms.events.SerializableMessage

sealed class CreateEventResult : SerializableMessage

data class CreateEventClashesResult(val userId: String) : CreateEventResult()

data class CreateEventSubmittedResult(val userId: String, val eventId: String) : CreateEventResult()