package com.anees.adventurehopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.anees.adventurehopper.ui.game.AdventureAppState
import com.anees.adventurehopper.ui.game.GameApp
import com.anees.adventurehopper.ui.theme.AdventureHopperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdventureHopperTheme {
                val appState = remember { AdventureAppState() }
                GameApp(appState)
            }
        }
    }
}
