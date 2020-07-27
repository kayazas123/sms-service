package com.porterhead.sms.event

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.porterhead.sms.domain.MessageStatus
import com.porterhead.sms.jpa.MessageRepository
import com.porterhead.sms.provider.ProviderException
import com.porterhead.sms.provider.ProviderRouter
import mu.KotlinLogging
import java.time.Instant
import java.util.*
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.transaction.Transactional

/**
 * Handle events that have been generated by the Debezium connector
 */
@ApplicationScoped
class SmsMessageCreatedHandler {

    companion object {
        private val gson: Gson = GsonBuilder().create()
    }

    private val log = KotlinLogging.logger {}

    @Inject
    lateinit var eventLog: EventLog

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var router: ProviderRouter

    @Transactional
    fun onEvent(eventId: UUID, eventType: String, key: String, payload: String, ts: Instant) {
        log.debug { "Event handler for Created messages invoked" }
        if (eventLog.alreadyProcessed(eventId)) {
            log.warn { "Message had already been handled $eventId" }
            return
        }
        val eventPayload = deserialize(payload)
        val messageId = UUID.fromString(eventPayload.get("id").asString)
        var message = messageRepository.findById(messageId)
        try {
            message = router.routeMessage(message)
        } catch (e: ProviderException) {
            log.debug ("Message will be marked as FAILED", e)
            message.status = MessageStatus.FAILED
        }
        message.updatedAt = Instant.now()
        messageRepository.persist(message)
        log.debug { "Message has been processed" }
    }

    private fun deserialize(eventMessage: String): JsonObject {
        log.debug { "attempting to deserialize message $eventMessage" }
        var payload = eventMessage //default
        try {
            if (eventMessage.contains("schema")) {
                //extract the payload
                val json: JsonObject = gson
                        .fromJson(eventMessage, JsonObject::class.java)
                payload = json.get("payload").asString
            }
            val unescaped = payload.replace("\\\"", "\"").trimIndent()
            val trimmed = unescaped.removeSurrounding("\"")
            val json = gson.fromJson(trimmed, JsonObject::class.java)
            log.debug { "payload has been parsed $json" }
            return json

        } catch (e: Exception) {
            log.error { "Error parsing payload $e" }
            throw e
        }

    }
}
