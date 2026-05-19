import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}


android {
    namespace = "com.dapurandia.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dapurandia.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "FONNTE_TOKEN",
            "\"${localProperties.getProperty("fonnte.token", "")}\""
        )
        buildConfigField(
            "String",
            "PASSWORD_RESET_REQUEST_URL",
            "\"${localProperties.getProperty("password.reset.request.url", "https://us-central1-dapurandiaapp.cloudfunctions.net/requestPasswordResetOtp")}\""
        )
        buildConfigField(
            "String",
            "PASSWORD_RESET_CONFIRM_URL",
            "\"${localProperties.getProperty("password.reset.confirm.url", "https://us-central1-dapurandiaapp.cloudfunctions.net/confirmPasswordResetOtp")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")// Tambahkan untuk cek versi dan paksa resolve
    implementation("com.google.firebase:firebase-firestore:24.10.3")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.maps.android:android-maps-utils:2.3.0")
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.github.f0ris.sweetalert:library:1.6.2")




}
