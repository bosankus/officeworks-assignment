package com.bosankus.officeworks.core.data.remote

import co.touchlab.kermit.Logger
import com.bosankus.officeworks.core.data.remote.dto.CategoryDto
import com.bosankus.officeworks.core.data.remote.dto.ProductDto
import com.bosankus.officeworks.core.data.source.ProductDataSource
import com.bosankus.officeworks.core.domain.result.DataResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodeURLPathPart

internal class RemoteProductDataSource(private val client: HttpClient, private val logger: Logger) : ProductDataSource {

    override suspend fun featured(): DataResult<List<ProductDto>> = safeApiCall(logger, "featured") { client.get("products/featured") }

    override suspend fun categories(): DataResult<List<CategoryDto>> = safeApiCall(logger, "categories") { client.get("categories") }

    override suspend fun productsInCategory(slug: String): DataResult<List<ProductDto>> = safeApiCall(logger, "productsInCategory") {
        // Encode the slug (including '/') so a bad value can't change the path.
        client.get("categories/${slug.encodeURLPathPart()}/products")
    }

    override suspend fun search(query: String): DataResult<List<ProductDto>> =
        safeApiCall(logger, "search") { client.get("products") { parameter("query", query) } }

    override suspend fun product(id: Int): DataResult<ProductDto> = safeApiCall(logger, "product") { client.get("products/$id") }
}
