package edu.cit.abelgas.localloop.features.postfavor.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for POST /api/favors
 *
 * Field names confirmed from backend FavorDetailDto shape:
 *   title        → String  (required, max 200)
 *   description  → String  (required)
 *   category     → String  (required — must match backend enum exactly)
 *   dateNeeded   → String? (optional, ISO-8601 "yyyy-MM-dd")
 *
 * Backend wraps the response in ApiResponse<FavorDetailDto>
 * Gson will serialize using camelCase (no naming policy override in ApiClient)
 */
data class CreateFavorRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("dateNeeded")
    val dateNeeded: String? = null   // null omitted by Gson if field excluded
)