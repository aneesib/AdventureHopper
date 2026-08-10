package com.anees.adventurehopper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.anees.adventurehopper.location.LocationService
import com.anees.adventurehopper.location.ApproximateLocation

private enum class LocationUiState {
    NOT_REQUESTED,
    DENIED,
    PERMANENTLY_DENIED,
    LOADING,
    LOCATED,
    UNAVAILABLE
}

@Composable
fun ElectricianRequestScreen(
    onBack: () -> Unit,
    onSearchElectricians: (ApproximateLocation) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var permissionRequested by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(context.hasCoarseLocationPermission()) }
    var locationState by remember {
        mutableStateOf(
            if (permissionGranted) LocationUiState.LOADING else LocationUiState.NOT_REQUESTED
        )
    }
    var approximateLocation by remember { mutableStateOf<ApproximateLocation?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequested = true
        permissionGranted = granted
        locationState = if (granted) LocationUiState.LOADING else LocationUiState.DENIED
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            locationState = LocationUiState.LOADING
            approximateLocation = LocationService.getApproximateLocation(context)
            locationState = if (approximateLocation != null) {
                LocationUiState.LOCATED
            } else {
                LocationUiState.UNAVAILABLE
            }
        }
    }

    val requestLocation = {
        if (permissionGranted) {
            locationState = LocationUiState.LOADING
        } else if (
            permissionRequested &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            locationState = LocationUiState.PERMANENTLY_DENIED
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4F8FC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "👨‍🔧 הזמנת חשמלאי",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF102A43)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "נוכל לעזור לך למצוא חשמלאי מוסמך באזור שלך.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF52606D)
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "כדי לחפש חשמלאים זמינים בקרבת מקום, נדרשת גישה לאזור המשוער שלך.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF334E68)
                )
                Spacer(modifier = Modifier.height(28.dp))
                LocationStatusContent(
                    state = locationState,
                    approximateLocation = approximateLocation,
                    onRequestLocation = requestLocation,
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    },
                    onSearchElectricians = onSearchElectricians
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "← חזרה", fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun LocationStatusContent(
    state: LocationUiState,
    approximateLocation: ApproximateLocation?,
    onRequestLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearchElectricians: (ApproximateLocation) -> Unit
) {
    when (state) {
        LocationUiState.NOT_REQUESTED -> {
            LocationActionButton(
                text = "📍 מצא חשמלאי קרוב אליי",
                onClick = onRequestLocation
            )
        }
        LocationUiState.DENIED -> {
            Text(
                text = "לא ניתנה הרשאת מיקום. אנו זקוקים לאזור המשוער שלך כדי לחפש חשמלאים בקרבת מקום.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB3261E)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationActionButton(
                text = "📍 נסה שוב",
                onClick = onRequestLocation
            )
        }
        LocationUiState.PERMANENTLY_DENIED -> {
            Text(
                text = "הרשאת המיקום חסומה. כדי להשתמש באזור המשוער שלך, יש להפעיל אותה בהגדרות האפליקציה.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB3261E)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "פתח הגדרות הרשאה", fontSize = 17.sp)
            }
        }
        LocationUiState.LOADING -> {
            Text(
                text = "מאתר את האזור המשוער שלך...",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF0B6E99)
            )
        }
        LocationUiState.LOCATED -> {
            Text(
                text = "📍 האזור שלך:\n${approximateLocation?.areaName ?: "אזור לא זוהה"}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF102A43)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "נוכל להשתמש באזור שלך כדי לחפש חשמלאים זמינים בקרבת מקום.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF334E68)
            )
            Spacer(modifier = Modifier.height(20.dp))
            LocationActionButton(
                text = "🔍 חפש חשמלאים באזור",
                onClick = {
                    approximateLocation?.let { location -> onSearchElectricians(location) }
                }
            )
        }
        LocationUiState.UNAVAILABLE -> {
            Text(
                text = "לא הצלחנו לקרוא כרגע את האזור המשוער שלך. ודא שהמיקום מופעל ונסה שוב.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFB3261E)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationActionButton(
                text = "📍 נסה שוב",
                onClick = onRequestLocation
            )
        }
    }
}

@Composable
private fun LocationActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0B6E99),
            contentColor = Color.White
        )
    ) {
        Text(text = text, fontSize = 17.sp)
    }
}

private fun Context.hasCoarseLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED