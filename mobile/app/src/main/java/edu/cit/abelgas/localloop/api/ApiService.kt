package edu.cit.abelgas.localloop.api

import edu.cit.abelgas.localloop.model.ApiResponse
import edu.cit.abelgas.localloop.model.AuthData
import edu.cit.abelgas.localloop.model.LoginRequest
import edu.cit.abelgas.localloop.model.RegisterRequest
import edu.cit.abelgas.localloop.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

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
}