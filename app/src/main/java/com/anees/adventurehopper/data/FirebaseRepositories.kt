package com.anees.adventurehopper.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.anees.adventurehopper.model.Electrician
import com.anees.adventurehopper.model.ElectricianServiceType
import com.anees.adventurehopper.model.ServiceRequest
import com.anees.adventurehopper.model.ServiceRequestStatus
import com.anees.adventurehopper.model.UserProfile
import com.anees.adventurehopper.model.UserRole
import kotlin.math.*

private const val MAX_ELECTRICIAN_DISTANCE_KM = 50.0

object FirebaseAvailability {
    fun app(context: Context): FirebaseApp? =
        FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)

    fun isConfigured(context: Context): Boolean = app(context) != null
}

class FirebaseAuthRepository(
    private val context: Context
) : AuthRepository {
    private val auth: FirebaseAuth?
        get() = FirebaseAvailability.app(context)?.let { FirebaseAuth.getInstance(it) }

    override fun currentUserId(): String? = auth?.currentUser?.uid

    override fun ensureSignedIn(onResult: (Result<String>) -> Unit) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return
        }
        firebaseAuth.currentUser?.let { user ->
            Log.d("FirebaseAuthFlow", "Existing Firebase user: ${user.uid}")
            onResult(Result.success(user.uid))
            return
        }
        firebaseAuth.signInAnonymously()
            .addOnSuccessListener {
                val authenticatedUser = firebaseAuth.currentUser
                Log.d(
                    "FirebaseAuthFlow",
                    "Anonymous sign-in completed: FirebaseAuth.currentUser=${authenticatedUser?.uid ?: "null"}"
                )
                authenticatedUser?.uid?.let { uid ->
                    onResult(Result.success(uid))
                } ?: onResult(
                    Result.failure(IllegalStateException("Firebase sign-in completed without a current user."))
                )
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun ensureAnonymousUser(onResult: (Result<UserProfile>) -> Unit) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return
        }
        val existing = firebaseAuth.currentUser
        if (existing != null) {
            loadOrCreateProfile(existing, onResult)
        } else {
            firebaseAuth.signInAnonymously()
                .addOnSuccessListener { result -> loadOrCreateProfile(result.user!!, onResult) }
                .addOnFailureListener { onResult(Result.failure(it)) }
        }
    }

    private fun loadOrCreateProfile(user: FirebaseUser, onResult: (Result<UserProfile>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val reference = firestore.collection("users").document(user.uid)
        reference.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onResult(Result.success(snapshot.toUserProfile(user.uid)))
                } else {
                    val profile = UserProfile(id = user.uid)
                    reference.set(
                        mapOf(
                            "id" to profile.id,
                            "displayName" to profile.displayName,
                            "phone" to profile.phone,
                            "role" to profile.role.name,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener { onResult(Result.success(profile)) }
                        .addOnFailureListener { onResult(Result.failure(it)) }
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}

class FirebaseElectricianRepository(
    private val context: Context
) : ElectricianRepository {
    private val firestore: FirebaseFirestore?
        get() = FirebaseAvailability.app(context)?.let { FirebaseFirestore.getInstance(it) }

    override fun findElectriciansNearLocation(
        latitude: Double,
        longitude: Double,
        serviceType: ElectricianServiceType,
        onResult: (Result<List<Electrician>>) -> Unit
    ) {
        val database = firestore
        if (database == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return
        }
        val authUser = FirebaseAvailability.app(context)?.let { FirebaseAuth.getInstance(it).currentUser }
        Log.d(
            "ElectricianSearch",
            "Before electricians query: FirebaseAuth.currentUser=${authUser?.uid ?: "null"}"
        )
        database.collection("electricians")
            .whereEqualTo("isAvailable", true)
            .whereEqualTo("isVerified", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val results = snapshot.documents.mapNotNull { it.toElectrician() }
                    .filter { serviceType.key in it.serviceTypes.map(ElectricianServiceType::key) }
                    .map { it.copy(distanceKm = distanceKm(latitude, longitude, it.latitude, it.longitude)) }
                    .filter { electrician ->
                        electrician.distanceKm?.let { distance ->
                            distance.isFinite() && distance <= MAX_ELECTRICIAN_DISTANCE_KM
                        } == true
                    }
                    .sortedBy { it.distanceKm }
                onResult(Result.success(results))
            }
            .addOnFailureListener {
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "Firestore electricians query failed; currentUser=${authUser?.uid ?: "null"}; ${it.message}",
                            it
                        )
                    )
                )
            }
    }

    override fun observeIncomingRequests(
        electricianId: String,
        onResult: (Result<List<ServiceRequest>>) -> Unit
    ): () -> Unit {
        val database = firestore
        if (database == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return {}
        }
        val listener = database.collection("serviceRequests")
            .whereEqualTo("status", ServiceRequestStatus.SENT.name)
            .whereEqualTo("assignedElectricianId", electricianId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) onResult(Result.failure(error))
                else onResult(Result.success(snapshot?.documents?.mapNotNull { it.toServiceRequest() }.orEmpty()))
            }
        return { listener.remove() }
    }

    override fun setAvailability(electricianId: String, isAvailable: Boolean, onResult: (Result<Unit>) -> Unit) {
        firestore?.collection("electricians")?.document(electricianId)
            ?.update("isAvailable", isAvailable)
            ?.addOnSuccessListener { onResult(Result.success(Unit)) }
            ?.addOnFailureListener { onResult(Result.failure(it)) }
            ?: onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
    }

    override fun acceptRequest(requestId: String, electricianId: String, onResult: (Result<Unit>) -> Unit) {
        updateRequestStatus(requestId, electricianId, ServiceRequestStatus.ACCEPTED, onResult)
    }

    override fun rejectRequest(requestId: String, electricianId: String, onResult: (Result<Unit>) -> Unit) {
        updateRequestStatus(requestId, electricianId, ServiceRequestStatus.REJECTED, onResult)
    }

    private fun updateRequestStatus(
        requestId: String,
        electricianId: String,
        status: ServiceRequestStatus,
        onResult: (Result<Unit>) -> Unit
    ) {
        val database = firestore
        if (database == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return
        }
        database.runTransaction { transaction ->
            val reference = database.collection("serviceRequests").document(requestId)
            val snapshot = transaction.get(reference)
            val currentStatus = snapshot.getString("status")
            if (status == ServiceRequestStatus.ACCEPTED && currentStatus != ServiceRequestStatus.SENT.name) {
                throw IllegalStateException("הבקשה כבר טופלה על ידי חשמלאי אחר.")
            }
            transaction.update(
                reference,
                mapOf(
                    "status" to status.name,
                    "assignedElectricianId" to if (status == ServiceRequestStatus.ACCEPTED) electricianId else null
                )
            )
        }.addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}

class FirebaseServiceRequestRepository(
    private val context: Context
) : ServiceRequestRepository {
    private val firestore: FirebaseFirestore?
        get() = FirebaseAvailability.app(context)?.let { FirebaseFirestore.getInstance(it) }

    override fun createRequest(request: ServiceRequest, onResult: (Result<ServiceRequest>) -> Unit) {
        val database = firestore
        if (database == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return
        }
        val reference = database.collection("serviceRequests").document()
        val initial = request.copy(id = reference.id, status = ServiceRequestStatus.CREATED)
        reference.set(initial.toFirestoreMap())
            .continueWithTask { reference.update("status", ServiceRequestStatus.SEARCHING.name) }
            .continueWithTask { reference.update("status", ServiceRequestStatus.SENT.name) }
            .addOnSuccessListener { onResult(Result.success(initial.copy(status = ServiceRequestStatus.SENT))) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun observeRequest(requestId: String, onResult: (Result<ServiceRequest>) -> Unit): () -> Unit {
        val database = firestore
        if (database == null) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return {}
        }
        val registration = database.collection("serviceRequests").document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) onResult(Result.failure(error))
                else if (snapshot?.exists() == true) onResult(Result.success(snapshot.toServiceRequest()!!))
                else onResult(Result.failure(NoSuchElementException("בקשת השירות לא נמצאה.")))
            }
        return { registration.remove() }
    }
}

class FirebaseNotificationRepository(
    private val context: Context
) : NotificationRepository {
    override fun registerCurrentDeviceToken(onResult: (Result<Unit>) -> Unit) {
        val userId = FirebaseAuthRepository(context).currentUserId()
        if (userId == null || !FirebaseAvailability.isConfigured(context)) {
            onResult(Result.failure(IllegalStateException("Firebase project configuration is missing.")))
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .collection("fcmTokens").document(token)
                    .set(mapOf("token" to token, "createdAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { onResult(Result.failure(it)) }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}

private fun DocumentSnapshot.toUserProfile(id: String) = UserProfile(
    id = id,
    displayName = getString("displayName").orEmpty(),
    phone = getString("phone").orEmpty(),
    role = runCatching { UserRole.valueOf(getString("role") ?: UserRole.CUSTOMER.name) }.getOrDefault(UserRole.CUSTOMER),
    createdAt = getTimestamp("createdAt")?.toDate()?.time
)

private fun DocumentSnapshot.toElectrician(): Electrician? = runCatching {
    Electrician(
        id = id,
        displayName = getString("displayName").orEmpty(),
        phone = getString("phone").orEmpty(),
        serviceAreas = (get("serviceAreas") as? List<*>)?.filterIsInstance<String>().orEmpty(),
        serviceTypes = (get("serviceTypes") as? List<*>)?.filterIsInstance<String>()
            ?.mapNotNull { value -> ElectricianServiceType.entries.find { it.key == value } }.orEmpty(),
        isAvailable = getBoolean("isAvailable") == true,
        isVerified = getBoolean("isVerified") == true,
        latitude = getDouble("latitude") ?: return null,
        longitude = getDouble("longitude") ?: return null,
        createdAt = getTimestamp("createdAt")?.toDate()?.time
    )
}.getOrNull()

private fun DocumentSnapshot.toServiceRequest(): ServiceRequest? = runCatching {
    ServiceRequest(
        id = id,
        customerId = getString("customerId").orEmpty(),
        diagnosticCategory = getString("diagnosticCategory").orEmpty(),
        diagnosticResult = getString("diagnosticResult").orEmpty(),
        customerArea = getString("customerArea").orEmpty(),
        latitude = getDouble("latitude") ?: return null,
        longitude = getDouble("longitude") ?: return null,
        createdAt = getTimestamp("createdAt")?.toDate()?.time,
        status = runCatching { ServiceRequestStatus.valueOf(getString("status") ?: ServiceRequestStatus.CREATED.name) }
            .getOrDefault(ServiceRequestStatus.CREATED),
        assignedElectricianId = getString("assignedElectricianId"),
        notes = getString("notes").orEmpty()
    )
}.getOrNull()

private fun ServiceRequest.toFirestoreMap() = mapOf(
    "id" to id,
    "customerId" to customerId,
    "diagnosticCategory" to diagnosticCategory,
    "diagnosticResult" to diagnosticResult,
    "customerArea" to customerArea,
    "latitude" to latitude,
    "longitude" to longitude,
    "createdAt" to FieldValue.serverTimestamp(),
    "status" to status.name,
    "assignedElectricianId" to assignedElectricianId,
    "notes" to notes
)

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
}