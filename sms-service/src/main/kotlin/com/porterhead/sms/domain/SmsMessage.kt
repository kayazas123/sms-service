package com.porterhead.sms.domain

import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity(name="sms.message")
data class SmsMessage(

        @Id
        var id: UUID = UUID.randomUUID(),

        @Column(nullable = true, name = "from_number")
        var fromNumber: String?,

        @Column(nullable = false, name = "to_number")
        var toNumber: String = "",

        @Column(nullable = false)
        var text: String = "",

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        var status: MessageStatus = MessageStatus.WAITING,

        @Column(nullable = false, name="created_at")
        var createdAt: Instant = Instant.now(),

        @Column(nullable = false, name = "updated_at")
        var updatedAt: Instant = Instant.now(),

        @Column(nullable = false)
        var provider: String = ""
)

