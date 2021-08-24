package com.cms.events.messages

import com.cms.events.Event
import com.cms.events.SerializableMessage

data class GetEventsFromPeriodResult(val events: List<Event>) : SerializableMessage
