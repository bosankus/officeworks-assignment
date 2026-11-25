package com.bosankus.officeworks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bosankus.officeworks.core.ui.theme.AppTheme
import com.bosankus.officeworks.navigation.AppNavHost

@Composable
fun App(modifier: Modifier = Modifier) {
    AppTheme {
        Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNavHost()
        }
    }
}
