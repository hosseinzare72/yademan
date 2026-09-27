// مسیر: Yademan/app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hossein.yademan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hossein.yademan"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            // v1.2: R8 روشن → اپ خیلی سبک‌تر و روان‌تر (نسخه debug در Compose ذاتاً کند و لگ‌دار است)
            isMinifyEnabled = true
            isShrinkResources = true
            // برای نصب مستقیم APK بدون ساخت keystore جدا؛ برای انتشار در مارکت keystore خودتان را بگذارید
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    // نصب Baseline Profileهای Compose → اجرای روان‌تر از همان اولین باز شدن
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room (با KSP)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager برای سینک و بازچینش آلارم‌ها
    implementation(libs.androidx.work.runtime.ktx)

    // ذخیره امن توکن
    implementation(libs.androidx.security.crypto)

    // شبکه و JSON
    implementation(libs.okhttp)
    implementation(libs.gson)
}
