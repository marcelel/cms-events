package com.cms.events

import akka.cluster.sharding.ShardRegion
import kotlin.math.abs

class EventShardingMessageExtractor(private val maxShards: Int) : ShardRegion.MessageExtractor {

    override fun entityId(message: Any?): String? {
        return if (message is EventActorSupervisor.Message) message.to else null
    }

    override fun entityMessage(message: Any?): Any? {
        return if (message is EventActorSupervisor.Message) message.eventMessage else null
    }

    override fun shardId(message: Any?): String? {
        return if (message is EventActorSupervisor.Message) abs(message.to.hashCode() % maxShards).toString()
        else null
    }
}