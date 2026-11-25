package com.bosankus.officeworks.feature.catalogue.di

import com.bosankus.officeworks.feature.catalogue.detail.ProductDetailViewModel
import com.bosankus.officeworks.feature.catalogue.home.HomeViewModel
import com.bosankus.officeworks.feature.catalogue.search.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val catalogueModule = module {
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> SearchViewModel(params.get(), get()) }
    viewModel { params -> ProductDetailViewModel(params.get(), get()) }
}
