package edu.cit.abelgas.localloop.features.profile.model

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// ProfileResponseDto
// Endpoint: GET /api/profile
// Maps to backend ProfileResponse.java fields exactly.
// ─────────────────────────────────────────────────────────────────────────────
data class ProfileResponseDto(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val barangay: String? = null,
    val role: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: String? = null,         // ISO-8601 — used for "Member since"
    val reputationScore: Int? = 0,
    val favorsPosted: Long? = 0,
    val favorsClaimed: Long? = 0,
    val favorsCompleted: Long? = 0,
    val hasPassword: Boolean = true        // false for Google-only accounts
)

// ─────────────────────────────────────────────────────────────────────────────
// ProfileUpdateRequest
// Endpoint: PUT /api/profile
// Matches backend ProfileUpdateRequest.java — all nullable, backend ignores
// null fields (only updates what is present).
// ─────────────────────────────────────────────────────────────────────────────
data class ProfileUpdateRequest(
    val name: String? = null,
    val currentPassword: String? = null,
    val barangay: String? = null,
    val newPassword: String? = null,
    val confirmPassword: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// RecentActivityItem
// Built on the client from getMyPostedFavors + getMyClaimedFavors responses.
// Each item represents one favor in the user's history timeline.
// ─────────────────────────────────────────────────────────────────────────────
data class RecentActivityItem(
    val id: Long,
    val title: String,
    val status: String,      // OPEN | CLAIMED | COMPLETED | EXPIRED
    val role: String,        // "Posted" | "Claimed"
    val dateDisplay: String? // ISO-8601 — formatted in the adapter
)