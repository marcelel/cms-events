package com.cms.events

import akka.Done
import java.util.concurrent.CompletableFuture

interface ActivityService {

    fun publish(activity: Activity): CompletableFuture<Done>
}