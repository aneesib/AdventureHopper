package com.anees.adventurehopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anees.adventurehopper.ui.screens.HomeScreen
import com.anees.adventurehopper.ui.screens.TroubleshootingScreen
import com.anees.adventurehopper.ui.theme.AdventureHopperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdventureHopperTheme {
                var showTroubleshooting by remember { mutableStateOf(false) }

                if (showTroubleshooting) {
                    TroubleshootingScreen(onBack = { showTroubleshooting = false })
                } else {
                    HomeScreen(onTroubleshootingClick = { showTroubleshooting = true })
                }
            }
        }
    }
}
