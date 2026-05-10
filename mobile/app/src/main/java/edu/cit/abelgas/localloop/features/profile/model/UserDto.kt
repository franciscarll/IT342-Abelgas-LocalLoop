package edu.cit.abelgas.localloop.features.profile.model

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val barangay: String,
    val role: String,
    val reputationScore: Int,
    val profileImageUrl: String?
)