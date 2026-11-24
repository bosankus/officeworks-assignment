package com.bosankus.officeworks.core.data.source

import com.bosankus.officeworks.core.data.remote.dto.CategoryDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryResponseDto
import com.bosankus.officeworks.core.data.remote.dto.ProductDto
import com.bosankus.officeworks.core.domain.result.DataResult

/**
 * Where product data comes from. Today that's our backend (API_CONTRACT.md); a cache or another
 * vendor would be another implementation, and the repository wouldn't change.
 */
internal interface ProductDataSource {
    suspend fun featured(): DataResult<List<ProductDto>>
    suspend fun categories(): DataResult<List<CategoryDto>>
    suspend fun productsInCategory(slug: String): DataResult<List<ProductDto>>
    suspend fun search(query: String): DataResult<List<ProductDto>>
    suspend fun product(id: Int): DataResult<ProductDto>
}

internal interface EnquiryDataSource {
    suspend fun submit(request: EnquiryRequestDto, idempotencyKey: String): DataResult<EnquiryResponseDto>
}
