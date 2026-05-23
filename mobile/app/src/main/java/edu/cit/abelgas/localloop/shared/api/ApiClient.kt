package edu.cit.abelgas.localloop.shared.api

import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://127.0.0.1:8080/api/"

    private var prefs: SharedPreferencesHelper? = null

    fun init(prefs: SharedPreferencesHelper) {
        this.prefs = prefs
    }

    // ── Auth interceptor ──────────────────────────────────────────────────────
    // Reads token fresh on EVERY request — this is the key fix.
    // Previously okHttpClient was built eagerly as a val property, so prefs
    // was always null when the interceptor captured it at construction time.
    // Now okHttpClient is lazy — it's only built after init() has been called.
    private val authInterceptor = okhttp3.Interceptor { chain ->
        val token = prefs?.getToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ── KEY FIX: okHttpClient is now lazy ────────────────────────────────────
    // Previously this was a plain `val` which meant it was constructed at
    // class-load time — before init(prefs) was ever called — so prefs was
    // always null inside the interceptor and no token was ever attached.
    // Making it lazy means it's only built when first accessed, which is
    // always after MainActivity calls ApiClient.init(prefs).
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}