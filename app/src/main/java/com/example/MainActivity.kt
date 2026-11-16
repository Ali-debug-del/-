package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.AppShieldApp
import com.example.ui.AppShieldViewModel
import com.example.ui.theme.AppShieldTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppShieldViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppShieldTheme {
                AppShieldApp(viewModel = viewModel)
            }
        }
    }
}
