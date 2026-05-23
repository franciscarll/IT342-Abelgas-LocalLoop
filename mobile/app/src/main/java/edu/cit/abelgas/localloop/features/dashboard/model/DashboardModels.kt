package edu.cit.abelgas.localloop.features.dashboard.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// Generic API wrapper — matches backend { success, data, error } envelope
// ─────────────────────────────────────────────────────────────────────────────
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val message: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Paginated wrapper — matches Spring Page<T> shape
// Used by: GET /favors, GET /announcements
// ─────────────────────────────────────────────────────────────────────────────
data class PagedResponse<T>(
    val content: List<T> = emptyList(),
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val number: Int = 0,           // current page index
    val size: Int = 0,
    val last: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// Weather
// Endpoint: GET /api/weather
// Web reads: weather.temperature / weather.temp
//            weather.condition   / weather.description
//            weather.humidity
//            weather.windSpeed   / weather.wind_speed
//            weather.feelsLike   / weather.feels_like / weather.temperature
// ─────────────────────────────────────────────────────────────────────────────
data class WeatherDto(
    // temperature — web tries both keys
    val temperature: Double? = null,
    val temp: Double? = null,

    // condition label — web tries both keys
    val condition: String? = null,
    val description: String? = null,

    // humidity — percentage integer
    val humidity: Int? = null,

    // wind speed — web tries both keys
    @SerializedName("windSpeed")
    val windSpeed: Double? = null,
    @SerializedName("wind_speed")
    val wind_speed: Double? = null,

    // feels like — web tries three fallbacks
    @SerializedName("feelsLike")
    val feelsLike: Double? = null,
    @SerializedName("feels_like")
    val feels_like: Double? = null,

    // barangay location label (optional — some backends include this)
    val barangay: String? = null
) {
    /** Resolved temperature, matching web: weather.temperature ?? weather.temp ?? 0 */
    val resolvedTemp: Double get() = temperature ?: temp ?: 0.0

    /** Resolved condition label */
    val resolvedCondition: String get() = condition ?: description ?: "Clear"

    /** Resolved wind speed */
    val resolvedWindSpeed: Double get() = windSpeed ?: wind_speed ?: 0.0

    /** Resolved feels-like, matching web fallback chain */
    val resolvedFeelsLike: Double get() = feelsLike ?: feels_like ?: temperature ?: temp ?: 0.0
}

// ─────────────────────────────────────────────────────────────────────────────
// Favor
// Endpoint: GET /api/favors?page=0&size=5&status=OPEN[&category=Errand]
// Web reads: favor.id, favor.title, favor.description, favor.category,
//            favor.requesterName, favor.requesterId, favor.barangay,
//            favor.createdAt
// ─────────────────────────────────────────────────────────────────────────────
data class FavorDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "Other",
    val status: String = "OPEN",

    val requesterId: Long? = null,
    val requesterName: String? = null,

    val barangay: String? = null,
    val createdAt: String? = null    // ISO-8601 string e.g. "2025-02-18T10:30:00"
)

// ─────────────────────────────────────────────────────────────────────────────
// Announcement
// Endpoint: GET /api/announcements?page=0&size=3
// Web reads: ann.id, ann.title, ann.createdAt / ann.date, ann.postedBy
// ─────────────────────────────────────────────────────────────────────────────
data class AnnouncementDto(
    val id: Long = 0,
    val title: String = "",
    val content: String? = null,
    val category: String? = null,
    @SerializedName("isPinned", alternate = ["pinned"])
    val isPinned: Boolean = false,
    val createdAt: String? = null,
    val date: String? = null,          // fallback key the web also checks
    val postedBy: String? = null
) {
    /** Resolved date — web: ann.createdAt || ann.date */
    val resolvedDate: String? get() = createdAt ?: date
    /** Resolved author — web: ann.postedBy || 'Admin' */
    val resolvedAuthor: String get() = postedBy ?: "Admin"
}

// ─────────────────────────────────────────────────────────────────────────────
// Reputation
// Endpoint: GET /api/users/me/reputation
// Web reads: reputation.reputationScore, reputation.favorsPosted,
//            reputation.favorsCompleted
// ─────────────────────────────────────────────────────────────────────────────
data class ReputationDto(
    val reputationScore: Int = 0,
    val favorsPosted: Int = 0,
    val favorsCompleted: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Claim — POST /api/favors/{id}/claim  (no request body, no response body needed)
// ─────────────────────────────────────────────────────────────────────────────
// (No DTO needed — response is success/error envelope only)

// ─────────────────────────────────────────────────────────────────────────────
// User — stored in SharedPreferences, mirrors web AuthContext user shape
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// FavorDetail
// Endpoint: GET /api/favors/{id}
// Extends FavorDto with extra fields from web FavorDetailPage.jsx
// ─────────────────────────────────────────────────────────────────────────────
data class FavorDetailDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "Other",
    val status: String = "OPEN",          // OPEN | CLAIMED | COMPLETED | EXPIRED

    val requesterId: Long? = null,
    val requesterName: String? = null,
    val claimerId: Long? = null,
    val claimerName: String? = null,

    val barangay: String? = null,
    val dateNeeded: String? = null,       // ISO-8601
    val createdAt: String? = null,        // ISO-8601 — web: favor.createdAt
    val claimedAt: String? = null,        // ISO-8601 — web: favor.claimedAt
    val completedAt: String? = null,      // ISO-8601 — web: favor.completedAt
    val updatedAt: String? = null         // ISO-8601 — fallback for claimedAt
)

// ─────────────────────────────────────────────────────────────────────────────
// RequesterStats
// Endpoint: GET /api/users/{id}/reputation
// Mirrors web requesterStats shape from FavorDetailPage.jsx
// ─────────────────────────────────────────────────────────────────────────────
data class RequesterStatsDto(
    val reputationScore: Int = 0,
    val favorsPosted: Int = 0,
    val favorsCompleted: Int = 0,
    val memberSince: String? = null       // ISO-8601 — web: requesterStats.memberSince
)
