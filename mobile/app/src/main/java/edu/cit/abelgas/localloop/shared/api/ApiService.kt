package edu.cit.abelgas.localloop.shared.api

import edu.cit.abelgas.localloop.features.auth.model.AuthData
import edu.cit.abelgas.localloop.features.auth.model.LoginRequest
import edu.cit.abelgas.localloop.features.auth.model.RegisterRequest
import edu.cit.abelgas.localloop.features.dashboard.model.AnnouncementDto
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDetailDto
import edu.cit.abelgas.localloop.features.dashboard.model.FavorDto
import edu.cit.abelgas.localloop.features.dashboard.model.PagedResponse
import edu.cit.abelgas.localloop.features.dashboard.model.ReputationDto
import edu.cit.abelgas.localloop.features.dashboard.model.RequesterStatsDto
import edu.cit.abelgas.localloop.features.dashboard.model.WeatherDto
import edu.cit.abelgas.localloop.features.profile.model.UserDto
import edu.cit.abelgas.localloop.shared.model.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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
}