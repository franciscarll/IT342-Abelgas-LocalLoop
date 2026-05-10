package edu.cit.abelgas.localloop.features.auth.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val barangay: String
)