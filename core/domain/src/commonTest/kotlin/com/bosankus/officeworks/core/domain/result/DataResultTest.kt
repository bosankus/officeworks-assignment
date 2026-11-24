package com.bosankus.officeworks.core.domain.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataResultTest {

    @Test
    fun `map transforms success`() {
        assertEquals(DataResult.Success(4), DataResult.Success(2).map { it * 2 })
    }

    @Test
    fun `map keeps failure untouched`() {
        val failure: DataResult<Int> = DataResult.Failure(DataError.Network)
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `onSuccess runs only for success`() {
        var seen: Int? = null
        DataResult.Success(1).onSuccess { seen = it }
        assertEquals(1, seen)

        seen = null
        DataResult.Failure(DataError.Server).onSuccess { seen = 1 }
        assertNull(seen)
    }

    @Test
    fun `onFailure runs only for failure`() {
        var seen: DataError? = null
        DataResult.Failure(DataError.NotFound).onFailure { seen = it }
        assertEquals(DataError.NotFound, seen)

        seen = null
        DataResult.Success(1).onFailure { seen = it }
        assertNull(seen)
    }
}
