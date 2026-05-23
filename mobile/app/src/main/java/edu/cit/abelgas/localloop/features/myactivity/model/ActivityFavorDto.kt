package edu.cit.abelgas.localloop.features.myactivity.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// ActivityFavorDto
// Matches FavorResponse from FavorService.toResponse()
// Used by: GET /api/favors/my-posted, GET /api/favors/my-claimed
// ─────────────────────────────────────────────────────────────────────────────
data class ActivityFavorDto(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "Other",
    val status: String = "OPEN",        // OPEN | CLAIMED | COMPLETED | EXPIRED

    val requesterId: Long? = null,
    val requesterName: String? = null,
    val claimerId: Long? = null,
    val claimerName: String? = null,

    val barangay: String? = null,
    val dateNeeded: String? = null,
    val createdAt: String? = null,
    val claimedAt: String? = null,
    val completedAt: String? = null,
    val updatedAt: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// ReputationDetailDto
// Matches ReputationResponse from backend — includes history list
// Used by: GET /api/users/me/reputation
// ─────────────────────────────────────────────────────────────────────────────
data class ReputationDetailDto(
    val userId: Long? = null,
    val name: String? = null,
    val reputationScore: Int = 0,
    val favorsPosted: Long = 0,
    val favorsCompleted: Long = 0,
    val memberSince: String? = null,
    val history: List<ReputationHistoryDto>? = null
)

data class ReputationHistoryDto(
    val points: Int = 0,
    val reason: String = "",
    val createdAt: String? = null
)