plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-parcelize")
}

android {
    namespace = "edu.cit.abelgas.localloop"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.cit.abelgas.localloop"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ── Your existing dependencies (unchanged) ─────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)

    // Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // ── NEW: needed for Dashboard screen ───────────────────────────────────
    // RecyclerView — favors list, category chips, announcements list
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CardView — white rounded cards on the dashboard
    implementation("androidx.cardview:cardview:1.0.0")

    // CoordinatorLayout — root layout of activity_dashboard.xml
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // LiveData — used in DashboardViewModel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Gson — for SharedPreferencesHelper saving/reading UserDto as JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // ── Testing (unchanged) ────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}