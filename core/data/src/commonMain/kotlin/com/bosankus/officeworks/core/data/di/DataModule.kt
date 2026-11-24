package com.bosankus.officeworks.core.data.di

import co.touchlab.kermit.Logger
import com.bosankus.officeworks.core.data.DataConfig
import com.bosankus.officeworks.core.data.platform.platformHttpEngine
import com.bosankus.officeworks.core.data.platform.platformIdGenerator
import com.bosankus.officeworks.core.data.remote.RemoteEnquiryDataSource
import com.bosankus.officeworks.core.data.remote.RemoteProductDataSource
import com.bosankus.officeworks.core.data.remote.createHttpClient
import com.bosankus.officeworks.core.data.repository.EnquiryRepositoryImpl
import com.bosankus.officeworks.core.data.repository.ProductRepositoryImpl
import com.bosankus.officeworks.core.data.source.EnquiryDataSource
import com.bosankus.officeworks.core.data.source.ProductDataSource
import com.bosankus.officeworks.core.domain.enquiry.IdGenerator
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Binds the data layer to the domain interfaces. Swapping a data source means changing one line here. */
fun dataModule(config: DataConfig): Module = module {
    single<IdGenerator> { platformIdGenerator() }
    single { createHttpClient(platformHttpEngine(), config.baseUrl, Logger.withTag("Http")) }

    single<ProductDataSource> { RemoteProductDataSource(get(), Logger.withTag("ProductApi")) }
    single<EnquiryDataSource> { RemoteEnquiryDataSource(get(), Logger.withTag("EnquiryApi")) }

    single<ProductRepository> { ProductRepositoryImpl(get()) }
    single<EnquiryRepository> { EnquiryRepositoryImpl(get(), Logger.withTag("Enquiry")) }
}
