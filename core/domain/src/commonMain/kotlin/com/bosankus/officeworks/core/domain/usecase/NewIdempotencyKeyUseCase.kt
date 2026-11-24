package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.enquiry.IdGenerator
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey

/** Called once per enquiry form, so every retry of that form reuses the same key. */
class NewIdempotencyKeyUseCase(private val idGenerator: IdGenerator) {
    operator fun invoke(): IdempotencyKey = IdempotencyKey(idGenerator.newId())
}
