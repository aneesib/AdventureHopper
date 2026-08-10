package com.anees.adventurehopper.model

enum class UserRole { CUSTOMER, ELECTRICIAN }

enum class ElectricianServiceType(val key: String, val label: String) {
    NO_ELECTRICITY("NO_ELECTRICITY", "אין חשמל"),
    OUTLET_PROBLEM("OUTLET_PROBLEM", "שקע לא עובד"),
    LIGHTING_PROBLEM("LIGHTING_PROBLEM", "תאורה לא עובדת"),
    BREAKER_OR_RCD("BREAKER_OR_RCD", "הפחת קופץ"),
    HEATING_OR_BURNING_SMELL("HEATING_OR_BURNING_SMELL", "ריח או חימום חשוד"),
    ELECTRICAL_APPLIANCE("ELECTRICAL_APPLIANCE", "מכשיר חשמלי"),
    OTHER("OTHER", "משהו אחר")
}

enum class ServiceRequestStatus(val label: String) {
    CREATED("הבקשה נוצרה"),
    SEARCHING("מחפשים חשמלאי מתאים"),
    SENT("הבקשה נשלחה לחשמלאי"),
    ACCEPTED("החשמלאי קיבל את הבקשה"),
    REJECTED("החשמלאי לא קיבל את הבקשה"),
    COMPLETED("הטיפול הסתיים"),
    CANCELLED("הבקשה בוטלה")
}

data class UserProfile(
    val id: String,
    val displayName: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.CUSTOMER,
    val createdAt: Long? = null
)

data class Electrician(
    val id: String,
    val displayName: String,
    val phone: String,
    val serviceAreas: List<String>,
    val serviceTypes: List<ElectricianServiceType>,
    val isAvailable: Boolean,
    val isVerified: Boolean,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long? = null,
    val distanceKm: Double? = null
)

data class ServiceRequest(
    val id: String,
    val customerId: String,
    val diagnosticCategory: String,
    val diagnosticResult: String,
    val customerArea: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long? = null,
    val status: ServiceRequestStatus = ServiceRequestStatus.CREATED,
    val assignedElectricianId: String? = null,
    val notes: String = ""
)

fun serviceTypeForCategory(categoryTitle: String): ElectricianServiceType = when {
    categoryTitle.contains("אין חשמל") -> ElectricianServiceType.NO_ELECTRICITY
    categoryTitle.contains("שקע") -> ElectricianServiceType.OUTLET_PROBLEM
    categoryTitle.contains("תאורה") -> ElectricianServiceType.LIGHTING_PROBLEM
    categoryTitle.contains("פחת") -> ElectricianServiceType.BREAKER_OR_RCD
    categoryTitle.contains("ריח") || categoryTitle.contains("חימום") -> ElectricianServiceType.HEATING_OR_BURNING_SMELL
    categoryTitle.contains("מכשיר") -> ElectricianServiceType.ELECTRICAL_APPLIANCE
    else -> ElectricianServiceType.OTHER
}