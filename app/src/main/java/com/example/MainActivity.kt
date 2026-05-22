package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.HealthRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support configuration
        enableEdgeToEdge()
        
        // Database & Repository Singletons instantiations (Simple Injection Pattern)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HealthRepository(
            dailyCheckinDao = database.dailyCheckinDao(),
            routineDao = database.routineDao()
        )
        
        val viewModelFactory = MainViewModelFactory(applicationContext, repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
