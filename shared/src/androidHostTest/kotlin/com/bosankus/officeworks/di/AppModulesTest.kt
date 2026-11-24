package com.bosankus.officeworks.di

import androidx.lifecycle.ViewModel
import com.bosankus.officeworks.core.data.DataConfig
import org.koin.core.annotation.KoinInternalApi
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertTrue

class AppModulesTest {

    /**
     * Builds every definition in the graph, so a missing binding fails here and not when a screen opens.
     * New definitions are picked up automatically; there's no list to keep in sync.
     * ViewModels are skipped: they need navigation arguments and the main thread. AppNavigationTest
     * builds each of them through this same graph on a device.
     */
    @OptIn(KoinInternalApi::class)
    @Test
    fun `every definition resolves`() {
        val koin = koinApplication { modules(appModules(DataConfig("http://localhost/"))) }.koin

        val definitions = koin.instanceRegistry.instances.values
            .map { it.beanDefinition }
            .distinct()
            .filterNot { ViewModel::class.java.isAssignableFrom(it.primaryType.java) }

        // Guards against the test passing because it found nothing to check.
        assertTrue(definitions.size > 10, "Only found ${definitions.size} definitions")
        definitions.forEach { definition ->
            runCatching { koin.get<Any>(definition.primaryType, definition.qualifier) }
                .onFailure { throw AssertionError("Couldn't build ${definition.primaryType.simpleName}", it) }
        }
    }
}
