package com.cms.events.messages

import com.cms.events.SerializableMessage

sealed class DeleteEventResult : SerializableMessage

data class DeleteEventNotFoundResult(val userId: String, val eventId: String) : DeleteEventResult()

data class DeleteEventDeletedResult(val userId: String, val eventId: String) : DeleteEventResult()