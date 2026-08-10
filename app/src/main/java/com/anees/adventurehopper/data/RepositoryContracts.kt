package com.anees.adventurehopper.data

import com.anees.adventurehopper.model.Electrician
import com.anees.adventurehopper.model.ElectricianServiceType
import com.anees.adventurehopper.model.ServiceRequest
import com.anees.adventurehopper.model.UserProfile

interface AuthRepository {
    fun ensureAnonymousUser(onResult: (Result<UserProfile>) -> Unit)
    fun currentUserId(): String?
}

interface ElectricianRepository {
    fun findElectriciansNearLocation(
        latitude: Double,
        longitude: Double,
        serviceType: ElectricianServiceType,
        onResult: (Result<List<Electrician>>) -> Unit
    )

    fun observeIncomingRequests(
        electricianId: String,
        onResult: (Result<List<ServiceRequest>>) -> Unit
    ): () -> Unit

    fun setAvailability(
        electricianId: String,
        isAvailable: Boolean,
        onResult: (Result<Unit>) -> Unit
    )

    fun acceptRequest(
        requestId: String,
        electricianId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun rejectRequest(
        requestId: String,
        electricianId: String,
        onResult: (Result<Unit>) -> Unit
    )
}

interface ServiceRequestRepository {
    fun createRequest(
        request: ServiceRequest,
        onResult: (Result<ServiceRequest>) -> Unit
    )

    fun observeRequest(
        requestId: String,
        onResult: (Result<ServiceRequest>) -> Unit
    ): () -> Unit
}

interface NotificationRepository {
    fun registerCurrentDeviceToken(onResult: (Result<Unit>) -> Unit)
}