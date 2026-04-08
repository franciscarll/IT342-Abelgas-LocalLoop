package edu.cit.abelgas.localloop.model

data class RegisterRequest(
        val name: String,
        val email: String,
        val password: String,
        val barangay: String
)

data class LoginRequest(
        val email: String,
        val password: String
)

data class ApiResponse<T>(
        val success: Boolean,
        val data: T?,
        val error: ApiError?,
        val timestamp: String?
)

data class ApiError(
        val code: String?,
        val message: String?,
        val details: Any?
)

data class AuthData(
        val user: UserDto,
        val accessToken: String
)

data class UserDto(
        val id: Long,
        val name: String,
        val email: String,
        val barangay: String,
        val role: String,
        val reputationScore: Int,
        val profileImageUrl: String?
)