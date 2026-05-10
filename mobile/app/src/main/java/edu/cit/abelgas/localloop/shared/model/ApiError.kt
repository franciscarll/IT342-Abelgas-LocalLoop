package edu.cit.abelgas.localloop.shared.model

data class ApiError(
    val code: String?,
    val message: String?,
    val details: Any?
)