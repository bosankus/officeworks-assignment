package com.bosankus.officeworks.core.data.repository

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.bosankus.officeworks.core.data.remote.dto.EnquiryResponseDto
import com.bosankus.officeworks.core.data.source.EnquiryDataSource
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class EnquiryRepositoryInteractionTest {

    private val source = mockk<EnquiryDataSource>()
    private val messages = mutableListOf<String>()
    private val logger = Logger(
        StaticConfig(
            logWriterList = listOf(
                object : LogWriter() {
                    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                        messages += message
                    }
                },
            ),
        ),
        "test",
    )
    private val enquiry = Enquiry(
        productId = ProductId(2),
        name = "Jane Citizen",
        email = "jane@example.com",
        phone = "0400 000 000",
        message = "Private question",
        idempotencyKey = IdempotencyKey("key-1"),
    )

    @Test
    fun `submits once with the enquiry's key`() = runTest {
        coEvery { source.submit(any(), any()) } returns DataResult.Success(EnquiryResponseDto("enq_1"))

        EnquiryRepositoryImpl(source, logger).submit(enquiry)

        coVerify(exactly = 1) { source.submit(match { it.productId == 2 }, "key-1") }
    }

    @Test
    fun `never logs customer details`() = runTest {
        coEvery { source.submit(any(), any()) } returns DataResult.Success(EnquiryResponseDto("enq_1"))

        EnquiryRepositoryImpl(source, logger).submit(enquiry)

        assertTrue(messages.isNotEmpty())
        val pii = listOf("Jane", "jane@example.com", "0400", "Private question")
        assertTrue(messages.none { message -> pii.any { it in message } }, "PII leaked: $messages")
    }
}
