package edu.cit.abelgas.localloop.shared.model

import edu.cit.abelgas.localloop.shared.model.ApiError

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
    val timestamp: String?
)