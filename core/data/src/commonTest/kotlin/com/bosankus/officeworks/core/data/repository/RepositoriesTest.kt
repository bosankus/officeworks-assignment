package com.bosankus.officeworks.core.data.repository

import com.bosankus.officeworks.core.data.mapper.toDomain
import com.bosankus.officeworks.core.data.remote.dto.CategoryDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryResponseDto
import com.bosankus.officeworks.core.data.remote.dto.ProductDto
import com.bosankus.officeworks.core.data.source.EnquiryDataSource
import com.bosankus.officeworks.core.data.source.ProductDataSource
import com.bosankus.officeworks.core.data.support.productDto
import com.bosankus.officeworks.core.data.support.silentLogger
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoriesTest {

    private val dto = productDto(1)
    private val category = CategoryDto("electronics", "Electronics")

    private val productSource = object : ProductDataSource {
        val calls = mutableListOf<String>()
        override suspend fun featured() = DataResult.Success(listOf(dto))
        override suspend fun categories() = DataResult.Success(listOf(category))
        override suspend fun productsInCategory(slug: String): DataResult<List<ProductDto>> {
            calls += "category:$slug"
            return DataResult.Success(listOf(dto))
        }
        override suspend fun search(query: String): DataResult<List<ProductDto>> {
            calls += "search:$query"
            return DataResult.Failure(DataError.Network)
        }
        override suspend fun product(id: Int): DataResult<ProductDto> {
            calls += "product:$id"
            return DataResult.Success(dto)
        }
    }
    private val products = ProductRepositoryImpl(productSource)

    @Test
    fun `product repository maps dtos to domain`() = runTest {
        assertEquals(DataResult.Success(listOf(dto.toDomain())), products.getFeaturedProducts())
        assertEquals(DataResult.Success(listOf(category.toDomain())), products.getCategories())
        assertEquals(DataResult.Success(listOf(dto.toDomain())), products.getProductsByCategory(CategorySlug("electronics")))
        assertEquals(DataResult.Success(dto.toDomain()), products.getProduct(ProductId(1)))
    }

    @Test
    fun `product repository passes arguments and failures through`() = runTest {
        products.getProductsByCategory(CategorySlug("electronics"))
        val search = products.searchProducts("bag")
        products.getProduct(ProductId(4))

        assertEquals(DataResult.Failure(DataError.Network), search)
        assertEquals(listOf("category:electronics", "search:bag", "product:4"), productSource.calls)
    }

    private val enquiry = Enquiry(ProductId(2), "Jane", "jane@example.com", null, null, IdempotencyKey("key-1"))

    private class RecordingEnquirySource(var result: DataResult<EnquiryResponseDto>) : EnquiryDataSource {
        val sent = mutableListOf<Pair<EnquiryRequestDto, String>>()
        override suspend fun submit(request: EnquiryRequestDto, idempotencyKey: String): DataResult<EnquiryResponseDto> {
            sent += request to idempotencyKey
            return result
        }
    }

    @Test
    fun `enquiry repository sends the request with its key`() = runTest {
        val source = RecordingEnquirySource(DataResult.Success(EnquiryResponseDto("enq_1")))

        val result = EnquiryRepositoryImpl(source, silentLogger).submit(enquiry)

        assertEquals(DataResult.Success(Unit), result)
        assertEquals(EnquiryRequestDto(2, "Jane", "jane@example.com") to "key-1", source.sent.single())
    }

    @Test
    fun `enquiry repository passes failures through`() = runTest {
        val source = RecordingEnquirySource(DataResult.Failure(DataError.RateLimited))

        assertEquals(DataResult.Failure(DataError.RateLimited), EnquiryRepositoryImpl(source, silentLogger).submit(enquiry))
    }
}
