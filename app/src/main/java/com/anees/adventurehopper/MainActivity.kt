package com.anees.adventurehopper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anees.adventurehopper.ui.diagnostic.DiagnosticCategory
import com.anees.adventurehopper.ui.diagnostic.DiagnosticResult
import com.anees.adventurehopper.ui.screens.DiagnosticResultScreen
import com.anees.adventurehopper.ui.screens.DiagnosticScreen
import com.anees.adventurehopper.ui.screens.ElectricianRequestScreen
import com.anees.adventurehopper.ui.screens.ElectricianSearchScreen
import com.anees.adventurehopper.ui.screens.ElectricianConfirmationScreen
import com.anees.adventurehopper.ui.screens.ServiceRequestStatusScreen
import com.anees.adventurehopper.ui.screens.HomeScreen
import com.anees.adventurehopper.ui.screens.TroubleshootingScreen
import com.anees.adventurehopper.ui.theme.AdventureHopperTheme

private sealed interface AppDestination {
    object Home : AppDestination
    object Troubleshooting : AppDestination
    data class Diagnostic(val category: DiagnosticCategory) : AppDestination
    data class Result(val category: DiagnosticCategory, val result: DiagnosticResult) : AppDestination
    data class ElectricianRequest(val result: Result) : AppDestination
    data class ElectricianSearch(val result: Result, val location: com.anees.adventurehopper.location.ApproximateLocation) : AppDestination
    data class ElectricianConfirmation(
        val result: Result,
        val location: com.anees.adventurehopper.location.ApproximateLocation,
        val electrician: com.anees.adventurehopper.model.Electrician
    ) : AppDestination
    data class RequestStatus(val request: com.anees.adventurehopper.model.ServiceRequest) : AppDestination
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdventureHopperTheme {
                var destination by remember { mutableStateOf<AppDestination>(AppDestination.Home) }

                when (val currentDestination = destination) {
                    AppDestination.Home -> {
                        HomeScreen(onTroubleshootingClick = {
                            destination = AppDestination.Troubleshooting
                        })
                    }
                    AppDestination.Troubleshooting -> {
                        TroubleshootingScreen(
                            onBack = { destination = AppDestination.Home },
                            onCategorySelected = { category ->
                                destination = AppDestination.Diagnostic(category)
                            }
                        )
                    }
                    is AppDestination.Diagnostic -> {
                        DiagnosticScreen(
                            category = currentDestination.category,
                            onBack = { destination = AppDestination.Troubleshooting },
                            onComplete = { answers, _ ->
                                destination = AppDestination.Result(
                                    category = currentDestination.category,
                                    result = currentDestination.category.resultFor(answers)
                                )
                            }
                        )
                    }
                    is AppDestination.Result -> {
                        DiagnosticResultScreen(
                            category = currentDestination.category,
                            result = currentDestination.result,
                            onRequestElectrician = {
                                destination = AppDestination.ElectricianRequest(currentDestination)
                            },
                            onBackToCategories = {
                                destination = AppDestination.Troubleshooting
                            },
                            onBackToHome = { destination = AppDestination.Home }
                        )
                    }
                    is AppDestination.ElectricianRequest -> {
                        ElectricianRequestScreen(
                            onBack = { destination = currentDestination.result },
                            onSearchElectricians = { location ->
                                destination = AppDestination.ElectricianSearch(currentDestination.result, location)
                            }
                        )
                    }
                    is AppDestination.ElectricianSearch -> {
                        ElectricianSearchScreen(
                            location = currentDestination.location,
                            category = currentDestination.result.category,
                            result = currentDestination.result.result,
                            onBack = { destination = currentDestination.result },
                            onSelectElectrician = { electrician ->
                                destination = AppDestination.ElectricianConfirmation(
                                    currentDestination.result,
                                    currentDestination.location,
                                    electrician
                                )
                            }
                        )
                    }
                    is AppDestination.ElectricianConfirmation -> {
                        ElectricianConfirmationScreen(
                            location = currentDestination.location,
                            category = currentDestination.result.category,
                            result = currentDestination.result.result,
                            electrician = currentDestination.electrician,
                            onBack = { destination = currentDestination.result },
                            onRequestCreated = { request -> destination = AppDestination.RequestStatus(request) }
                        )
                    }
                    is AppDestination.RequestStatus -> {
                        ServiceRequestStatusScreen(
                            request = currentDestination.request,
                            onBack = { destination = AppDestination.Home }
                        )
                    }
                }
            }
        }
    }
}
