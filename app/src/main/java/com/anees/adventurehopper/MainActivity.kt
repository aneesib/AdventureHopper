package com.anees.adventurehopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anees.adventurehopper.ui.screens.HomeScreen
import com.anees.adventurehopper.ui.theme.AdventureHopperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdventureHopperTheme {
                HomeScreen()
            }
        }
    }
}
