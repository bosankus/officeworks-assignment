package com.bosankus.officeworks.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.bosankus.officeworks.core.data.DataConfig
import com.bosankus.officeworks.core.data.di.dataModule
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.usecase.GetCategoriesUseCase
import com.bosankus.officeworks.core.domain.usecase.GetCategoryPageUseCase
import com.bosankus.officeworks.core.domain.usecase.GetFeaturedProductsUseCase
import com.bosankus.officeworks.core.domain.usecase.GetProductUseCase
import com.bosankus.officeworks.core.domain.usecase.NewIdempotencyKeyUseCase
import com.bosankus.officeworks.core.domain.usecase.SearchProductsUseCase
import com.bosankus.officeworks.core.domain.usecase.SubmitEnquiryUseCase
import com.bosankus.officeworks.feature.catalogue.di.catalogueModule
import com.bosankus.officeworks.feature.enquiry.di.enquiryModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

// Domain stays free of DI, so its use cases are wired here.
internal val domainModule = module {
    factory { GetFeaturedProductsUseCase(get()) }
    factory { GetCategoriesUseCase(get()) }
    factory { GetCategoryPageUseCase(get()) }
    factory { SearchProductsUseCase(get()) }
    factory { GetProductUseCase(get()) }
    factory { NewIdempotencyKeyUseCase(get()) }
    factory { EnquiryFormValidator() }
    factory { SubmitEnquiryUseCase(get(), get()) }
}

internal fun appModules(config: DataConfig): List<Module> = listOf(dataModule(config), domainModule, catalogueModule, enquiryModule)

/** Called once from the platform entry point, before any UI. */
fun initApp(baseUrl: String, isDebug: Boolean) {
    Logger.setMinSeverity(if (isDebug) Severity.Debug else Severity.Warn)
    startKoin {
        modules(appModules(DataConfig(baseUrl = baseUrl)))
    }
}
