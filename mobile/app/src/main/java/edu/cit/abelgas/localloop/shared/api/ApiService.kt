package edu.cit.abelgas.localloop.shared.api

import edu.cit.abelgas.localloop.features.auth.model.AuthData
import edu.cit.abelgas.localloop.features.auth.model.GoogleAuthRequest
import edu.cit.abelgas.localloop.features.auth.model.LoginRequest
import edu.cit.abelgas.localloop.features.auth.model.RegisterRequest
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDetailDto
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDto
import edu.cit.abelgas.localloop.features.dashboard.model.PagedResponse
import edu.cit.abelgas.localloop.features.dashboard.model.ReputationDto
import edu.cit.abelgas.localloop.features.dashboard.model.RequesterStatsDto
import edu.cit.abelgas.localloop.features.dashboard.model.WeatherDto
import edu.cit.abelgas.localloop.features.profile.model.ProfileResponseDto
import edu.cit.abelgas.localloop.features.profile.model.ProfileUpdateRequest
import edu.cit.abelgas.localloop.features.profile.model.UserDto
import edu.cit.abelgas.localloop.shared.model.ApiResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // =========================================================================
    // AUTH — matches your existing AuthApiService exactly
    // =========================================================================

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthData>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthData>>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>

    @GET("auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserDto>>

    // =========================================================================
    // WEATHER — GET /api/weather
    // =========================================================================

    @GET("weather")
    suspend fun getWeather(): Response<ApiResponse<WeatherDto>>

    // =========================================================================
    // FAVORS — GET /api/favors?page=N&size=5&status=OPEN[&category=X]
    // =========================================================================

    @GET("favors")
    suspend fun getFavors(
        @Query("page")     page: Int,
        @Query("size")     size: Int,
        @Query("status")   status: String = "OPEN",
        @Query("category") category: String? = null
    ): Response<ApiResponse<PagedResponse<FavorDto>>>

    @POST("favors/{id}/claim")
    suspend fun claimFavor(
        @Path("id") favorId: Long
    ): Response<ApiResponse<Unit>>

    // =========================================================================
    // ANNOUNCEMENTS — GET /api/announcements?page=0&size=3
    // =========================================================================

    @GET("announcements")
    suspend fun getAnnouncements(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 3
    ): Response<ApiResponse<PagedResponse<AnnouncementDto>>>

    // =========================================================================
    // REPUTATION — GET /api/users/me/reputation
    // =========================================================================

    @GET("users/me/reputation")
    suspend fun getMyReputation(): Response<ApiResponse<ReputationDto>>

    // =========================================================================
    // FAVOR DETAIL — GET /api/favors/{id}
    // Mirrors web: api.get(`/favors/${id}`)
    // =========================================================================
    @GET("favors/{id}")
    suspend fun getFavorById(
        @Path("id") favorId: Long
    ): Response<ApiResponse<FavorDetailDto>>

    // =========================================================================
    // REQUESTER STATS — GET /api/users/{id}/reputation
    // Mirrors web: api.get(`/users/${favor.requesterId}/reputation`)
    // =========================================================================
    @GET("users/{id}/reputation")
    suspend fun getUserReputation(
        @Path("id") userId: Long
    ): Response<ApiResponse<RequesterStatsDto>>

    // =========================================================================
    // CLAIM FAVOR — POST /api/favors/{id}/claim
    // Already exists but needs FavorDetailDto response for detail screen
    // =========================================================================
    @POST("favors/{id}/claim")
    suspend fun claimFavorDetail(
        @Path("id") favorId: Long
    ): Response<ApiResponse<FavorDetailDto>>

    // =========================================================================
    // POST FAVOR — POST /api/favors
    // Request body:  CreateFavorRequest (title, description, category, dateNeeded?)
    // Response body: ApiResponse<FavorDetailDto>
    // Auth:          Bearer token injected automatically by ApiClient interceptor
    // =========================================================================
    @POST("favors")
    suspend fun postFavor(
        @Body request: edu.cit.abelgas.localloop.features.postfavor.model.CreateFavorRequest
    ): Response<ApiResponse<FavorDetailDto>>

    // =========================================================================
    // MY ACTIVITY — GET /api/favors/my-posted
    // Returns all favors the current user posted (all statuses)
    // =========================================================================
    @GET("favors/my-posted")
    suspend fun getMyPostedFavors(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<PagedResponse<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>>>

    // =========================================================================
    // MY ACTIVITY — GET /api/favors/my-claimed
    // Returns CLAIMED + COMPLETED favors where user is the claimer
    // =========================================================================
    @GET("favors/my-claimed")
    suspend fun getMyClaimedFavors(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<PagedResponse<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>>>

    // =========================================================================
    // MY ACTIVITY — DELETE /api/favors/{id}
    // Only OPEN favors. Only the requester.
    // =========================================================================
    @DELETE("favors/{id}")
    suspend fun deleteFavor(
        @Path("id") favorId: Long
    ): Response<ApiResponse<Unit>>

    // =========================================================================
    // MY ACTIVITY — PUT /api/favors/{id}/complete
    // Only the requester. Only CLAIMED favors. Awards +1 rep to claimer.
    // =========================================================================
    @PUT("favors/{id}/complete")
    suspend fun completeFavor(
        @Path("id") favorId: Long
    ): Response<ApiResponse<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>>

    // =========================================================================
    // MY ACTIVITY — PUT /api/favors/{id}/reopen
    // Only the requester. Only CLAIMED favors. Deducts -2 rep from claimer.
    // =========================================================================
    @PUT("favors/{id}/reopen")
    suspend fun reopenFavor(
        @Path("id") favorId: Long
    ): Response<ApiResponse<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>>

    // =========================================================================
    // MY ACTIVITY — PUT /api/favors/{id}/cancel-claim
    // Only the claimer. Only CLAIMED favors. Deducts -1 rep from claimer.
    // =========================================================================
    @PUT("favors/{id}/cancel-claim")
    suspend fun cancelClaim(
        @Path("id") favorId: Long
    ): Response<ApiResponse<edu.cit.abelgas.localloop.features.myactivity.model.ActivityFavorDto>>

    // ── Profile ───────────────────────────────────────────────────────────────

    /**
     * GET /api/profile
     * Returns full user profile (name, email, barangay, reputation, stats,
     * profileImageUrl, createdAt, hasPassword).
     */
    @GET("profile")
    suspend fun getProfile(): retrofit2.Response<ApiResponse<ProfileResponseDto>>

    @PUT("users/profile")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequest
    ): retrofit2.Response<ApiResponse<UserDto>>

    @Multipart
    @POST("profile/upload")
    suspend fun uploadProfilePhoto(
        @Part file: MultipartBody.Part
    ): retrofit2.Response<ApiResponse<UserDto>>

    // ── ADD THIS ──────────────────────────────────────────────────────────────
    @POST("auth/mobile/google")
    suspend fun googleSignIn(
        @Body request: GoogleAuthRequest
    ): Response<ApiResponse<AuthData>>

}