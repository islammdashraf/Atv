package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.IptvDatabase
import com.example.data.IptvRepository
import com.example.ui.IptvViewModel
import com.example.ui.IptvViewModelFactory
import com.example.ui.screens.MainIptvScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Simple, clean constructor injection for ViewModel as per Room integration instructions
    private val viewModel: IptvViewModel by viewModels {
        val database = IptvDatabase.getDatabase(applicationContext)
        val repository = IptvRepository(database.iptvDao())
        IptvViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainIptvScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
