package com.bosankus.officeworks.feature.enquiry.di

import com.bosankus.officeworks.feature.enquiry.EnquiryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val enquiryModule = module {
    viewModel { params -> EnquiryViewModel(params.get(), get(), get()) }
}
