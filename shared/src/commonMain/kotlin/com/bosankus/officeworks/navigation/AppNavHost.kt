package com.bosankus.officeworks.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bosankus.officeworks.feature.catalogue.detail.ProductDetailScreen
import com.bosankus.officeworks.feature.catalogue.home.HomeScreen
import com.bosankus.officeworks.feature.catalogue.search.SearchScreen
import com.bosankus.officeworks.feature.enquiry.EnquiryScreen

/** The only place that knows how features connect. Features expose callbacks, never each other's screens. */
@Composable
internal fun AppNavHost() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.navigateUp() }

    NavHost(navController, startDestination = HomeDestination) {
        composable<HomeDestination> {
            HomeScreen(
                onSearch = { navController.navigate(SearchDestination(it)) },
                onProductClick = { navController.navigate(ProductDestination(it)) },
            )
        }
        composable<SearchDestination> { entry ->
            SearchScreen(
                query = entry.toRoute<SearchDestination>().query,
                onBack = back,
                onProductClick = { navController.navigate(ProductDestination(it)) },
            )
        }
        composable<ProductDestination> { entry ->
            ProductDetailScreen(
                productId = entry.toRoute<ProductDestination>().productId,
                onBack = back,
                onEnquire = { id, title -> navController.navigate(EnquiryDestination(id, title)) },
            )
        }
        composable<EnquiryDestination> { entry ->
            val route = entry.toRoute<EnquiryDestination>()
            EnquiryScreen(productId = route.productId, productTitle = route.productTitle, onBack = back)
        }
    }
}
