package com.bosankus.officeworks.core.data.platform

import com.bosankus.officeworks.core.domain.enquiry.IdGenerator
import io.ktor.client.engine.HttpClientEngine

internal expect fun platformHttpEngine(): HttpClientEngine

internal expect fun platformIdGenerator(): IdGenerator
