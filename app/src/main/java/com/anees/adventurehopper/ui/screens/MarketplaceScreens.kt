package com.anees.adventurehopper.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anees.adventurehopper.data.FirebaseAuthRepository
import com.anees.adventurehopper.data.FirebaseElectricianRepository
import com.anees.adventurehopper.data.FirebaseServiceRequestRepository
import com.anees.adventurehopper.data.ServiceRequestRepository
import com.anees.adventurehopper.data.ElectricianRepository
import com.anees.adventurehopper.data.AuthRepository
import com.anees.adventurehopper.location.ApproximateLocation
import com.anees.adventurehopper.model.Electrician
import com.anees.adventurehopper.model.ElectricianServiceType
import com.anees.adventurehopper.model.ServiceRequest
import com.anees.adventurehopper.model.ServiceRequestStatus
import com.anees.adventurehopper.ui.diagnostic.DiagnosticCategory
import com.anees.adventurehopper.ui.diagnostic.DiagnosticResult
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

private val primaryBlue = Color(0xFF0B6E99)
private val ink = Color(0xFF102A43)
private val canvas = Color(0xFFF4F8FC)

@Composable
fun ElectricianSearchScreen(
    location: ApproximateLocation,
    category: DiagnosticCategory,
    result: DiagnosticResult,
    onBack: () -> Unit,
    onSelectElectrician: (Electrician) -> Unit
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<SearchState>(SearchState.Loading) }
    var searchAttempt by remember { mutableStateOf(0) }
    LaunchedEffect(location, category, searchAttempt) {
        val auth = FirebaseAuthRepository(context)
        auth.ensureSignedIn { authResult ->
            authResult.onSuccess {
                FirebaseElectricianRepository(context).findElectriciansNearLocation(
                    location.latitude,
                    location.longitude,
                    category.serviceType(),
                    onResult = { resultValue ->
                        state = resultValue.fold(
                            onSuccess = { electricians ->
                                if (electricians.isEmpty()) SearchState.Empty else SearchState.Success(electricians)
                            },
                            onFailure = { SearchState.Error(it.userMessage()) }
                        )
                    }
                )
            }.onFailure {
                val currentUser = FirebaseAuthRepository(context).currentUserId() ?: "null"
                state = SearchState.Error("${it.userMessage()} FirebaseAuth.currentUser=$currentUser")
            }
        }
    }

    MarketplaceSurface {
        MarketplaceHeader(title = "🔍 חיפוש חשמלאים באזור", onBack = onBack)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "📍 האזור שלך\n${location.areaName.ifBlank { "אזור לא זוהה" }}",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ink
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "מחפשים חשמלאים מאומתים וזמינים עבור ${category.title}.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF334E68)
        )
        Spacer(modifier = Modifier.height(24.dp))
        when (val currentState = state) {
            SearchState.Loading -> LoadingContent("מחפשים חשמלאים באזור...")
            SearchState.Empty -> EmptySearchContent(onBack = onBack, onRetry = { searchAttempt++ })
            is SearchState.Error -> ErrorContent(currentState.message, onRetry = { searchAttempt++ })
            is SearchState.Success -> currentState.electricians.forEach { electrician ->
                ElectricianCard(electrician = electrician, onClick = { onSelectElectrician(electrician) })
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ElectricianConfirmationScreen(
    location: ApproximateLocation,
    category: DiagnosticCategory,
    result: DiagnosticResult,
    electrician: Electrician,
    onBack: () -> Unit,
    onRequestCreated: (ServiceRequest) -> Unit
) {
    val context = LocalContext.current
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    MarketplaceSurface {
        MarketplaceHeader(title = "שליחת בקשה לחשמלאי", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        SummaryCard("סוג התקלה", category.title)
        SummaryCard("תוצאת הבדיקה", result.title)
        SummaryCard("האזור שלך", location.areaName.ifBlank { "אזור לא זוהה" })
        SummaryCard("החשמלאי שנבחר", electrician.displayName)
        Spacer(modifier = Modifier.height(24.dp))
        errorMessage?.let {
            Text(text = it, modifier = Modifier.fillMaxWidth(), color = Color(0xFFB3261E))
            Spacer(modifier = Modifier.height(12.dp))
        }
        Button(
            onClick = {
                if (sending) return@Button
                sending = true
                val auth: AuthRepository = FirebaseAuthRepository(context)
                auth.ensureAnonymousUser { authResult ->
                    authResult.fold(
                        onSuccess = { profile ->
                            val request = ServiceRequest(
                                id = "",
                                customerId = profile.id,
                                diagnosticCategory = category.title,
                                diagnosticResult = result.title,
                                customerArea = location.areaName,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                assignedElectricianId = electrician.id,
                                notes = result.safetyRecommendation
                            )
                            val repository: ServiceRequestRepository = FirebaseServiceRequestRepository(context)
                            repository.createRequest(request) { requestResult ->
                                sending = false
                                requestResult.fold(
                                    onSuccess = onRequestCreated,
                                    onFailure = { errorMessage = it.userMessage() }
                                )
                            }
                        },
                        onFailure = {
                            sending = false
                            errorMessage = it.userMessage()
                        }
                    )
                }
            },
            enabled = !sending,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
        ) {
            if (sending) CircularProgressIndicator(color = Color.White) else Text("📨 שלח בקשה", fontSize = 18.sp)
        }
    }
}

@Composable
fun ServiceRequestStatusScreen(
    request: ServiceRequest,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentRequest by remember { mutableStateOf(request) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    DisposableEffect(request.id) {
        val removeListener = FirebaseServiceRequestRepository(context).observeRequest(request.id) { result ->
            result.fold(
                onSuccess = { currentRequest = it },
                onFailure = { errorMessage = it.userMessage() }
            )
        }
        onDispose { removeListener() }
    }

    MarketplaceSurface {
        MarketplaceHeader(title = "סטטוס הבקשה", onBack = onBack)
        Spacer(modifier = Modifier.height(24.dp))
        SummaryCard("סוג התקלה", currentRequest.diagnosticCategory)
        SummaryCard("האזור", currentRequest.customerArea)
        SummaryCard("סטטוס", currentRequest.status.label)
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, modifier = Modifier.fillMaxWidth(), color = Color(0xFFB3261E))
        }
    }
}

@Composable
fun ElectricianHomeScreen(
    electricianId: String,
    displayName: String,
    serviceAreas: List<String>,
    onOpenRequests: () -> Unit
) {
    val context = LocalContext.current
    var available by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    MarketplaceSurface {
        MarketplaceHeader(title = "מסך החשמלאי", onBack = {})
        Spacer(modifier = Modifier.height(24.dp))
        Text("שלום, $displayName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
        Spacer(modifier = Modifier.height(16.dp))
        Text("אזורי שירות: ${serviceAreas.joinToString(", ").ifBlank { "לא הוגדרו" }}", modifier = Modifier.fillMaxWidth(), color = Color(0xFF334E68))
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                FirebaseElectricianRepository(context).setAvailability(electricianId, !available) {
                    it.onSuccess { available = !available }
                        .onFailure { message = it.userMessage() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (available) Color(0xFF2E7D32) else Color(0xFF52606D))
        ) { Text(if (available) "זמין לקבלת בקשות" else "לא זמין לקבלת בקשות") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenRequests, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp)) {
            Text("בקשות נכנסות")
        }
        message?.let { Spacer(modifier = Modifier.height(12.dp)); Text(it, color = Color(0xFFB3261E)) }
    }
}

@Composable
fun IncomingRequestsScreen(
    electricianId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var requests by remember { mutableStateOf<List<ServiceRequest>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(electricianId) {
        FirebaseElectricianRepository(context).observeIncomingRequests(electricianId) { result ->
            result.fold(onSuccess = { requests = it }, onFailure = { message = it.userMessage() })
        }
    }
    MarketplaceSurface {
        MarketplaceHeader(title = "בקשות נכנסות", onBack = onBack)
        Spacer(modifier = Modifier.height(20.dp))
        if (requests.isEmpty()) {
            Text("אין כרגע בקשות נכנסות.", modifier = Modifier.fillMaxWidth(), color = Color(0xFF52606D))
        }
        requests.forEach { request ->
            RequestCard(request = request, electricianId = electricianId, onMessage = { message = it })
            Spacer(modifier = Modifier.height(12.dp))
        }
        message?.let { Text(it, modifier = Modifier.fillMaxWidth(), color = Color(0xFFB3261E)) }
    }
}

@Composable
private fun RequestCard(request: ServiceRequest, electricianId: String, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(request.diagnosticCategory, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
            Spacer(modifier = Modifier.height(8.dp))
            Text("אזור: ${request.customerArea}", color = Color(0xFF334E68))
            Text(request.diagnosticResult, color = Color(0xFF334E68))
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    FirebaseElectricianRepository(context).acceptRequest(request.id, electricianId) { it.onFailure { error -> onMessage(error.userMessage()) } }
                }) { Text("✓ קבל בקשה") }
                OutlinedButton(onClick = {
                    FirebaseElectricianRepository(context).rejectRequest(request.id, electricianId) { it.onFailure { error -> onMessage(error.userMessage()) } }
                }) { Text("✕ דחה") }
            }
        }
    }
}

@Composable
private fun MarketplaceSurface(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(modifier = Modifier.fillMaxSize(), color = canvas) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
private fun MarketplaceHeader(title: String, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        OutlinedButton(onClick = onBack) { Text("← חזרה") }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ink)
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = primaryBlue)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = ink)
        }
    }
}

@Composable
private fun ElectricianCard(electrician: Electrician, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(electrician.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
            Text("אזורי שירות: ${electrician.serviceAreas.joinToString(", ").ifBlank { "לא צוין" }}", color = Color(0xFF334E68))
            Text("זמין", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            if (electrician.isVerified) Text("מאומת", color = primaryBlue, fontWeight = FontWeight.Bold)
            Text("שירות: ${electrician.serviceTypes.joinToString(", ") { it.label }}", color = Color(0xFF334E68))
            electrician.distanceKm?.let { Text("מרחק משוער: ${"%.1f".format(it)} ק״מ", color = Color(0xFF52606D)) }
        }
    }
}

@Composable
private fun LoadingContent(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = primaryBlue)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text, color = primaryBlue)
    }
}

@Composable
private fun EmptySearchContent(onBack: () -> Unit, onRetry: () -> Unit) {
    Text("כרגע לא נמצאו חשמלאים זמינים באזור שלך.", modifier = Modifier.fillMaxWidth(), color = Color(0xFF52606D))
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("נסה שוב") }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("בחירת תקלה אחרת") }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Text(message, modifier = Modifier.fillMaxWidth(), color = Color(0xFFB3261E))
    Spacer(modifier = Modifier.height(20.dp))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("נסה שוב") }
}

private sealed interface SearchState {
    object Loading : SearchState
    object Empty : SearchState
    data class Success(val electricians: List<Electrician>) : SearchState
    data class Error(val message: String) : SearchState
}

private fun DiagnosticCategory.serviceType(): ElectricianServiceType =
    com.anees.adventurehopper.model.serviceTypeForCategory(title)

private fun Throwable.userMessage(): String =
    when {
        message?.contains("configuration", ignoreCase = true) == true ->
            "Firebase עדיין לא מוגדר באפליקציה. יש להוסיף google-services.json כדי להפעיל את השירות."
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "אין הרשאה לקרוא חשמלאים. יש לוודא שהכללים מאפשרים למשתמש מחובר לקרוא electricians מאומתים וזמינים."
        this is FirebaseAuthException && errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
            "המשתמש לא מחובר ל-Firebase. יש להפעיל Anonymous Authentication בפרויקט."
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore דורש הגדרה נוספת בפרויקט Firebase. בדוק שהמסד פעיל והשאילתה נתמכת."
        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "שירות Firebase אינו זמין כרגע. בדוק את חיבור האינטרנט ונסה שוב."
        else -> "אירעה שגיאה ב-Firebase (${this::class.simpleName}: ${message ?: "ללא פרטים"}). נסה שוב."
    }
