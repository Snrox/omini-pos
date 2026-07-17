package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.db.POSDatabase
import com.example.data.repository.POSRepository
import com.example.ui.screens.POSAppContent
import com.example.ui.theme.OmniPOSTheme
import com.example.viewmodel.POSViewModel

class MainActivity : ComponentActivity() {
    private lateinit var database: POSDatabase
    private lateinit var repository: POSRepository
    private lateinit var viewModel: POSViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize local SQLite Room Database
        database = Room.databaseBuilder(
            applicationContext,
            POSDatabase::class.java,
            "omnipos_enterprise.db"
        ).fallbackToDestructiveMigration().build()

        // 2. Initialize Repository and ViewModel
        repository = POSRepository(database)
        viewModel = POSViewModel(repository, applicationContext)

        enableEdgeToEdge()
        
        setContent {
            OmniPOSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    POSAppContent(viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            viewModel.syncEngine.shutdown()
        }
        super.onDestroy()
    }
}

