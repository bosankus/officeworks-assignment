package com.bosankus.officeworks.core.data.platform

import com.bosankus.officeworks.core.domain.enquiry.IdGenerator
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.util.UUID

internal actual fun platformHttpEngine(): HttpClientEngine = OkHttp.create()

internal actual fun platformIdGenerator(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }
