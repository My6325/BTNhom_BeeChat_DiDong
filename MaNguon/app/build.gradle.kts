plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.beechats"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.beechats"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "ZEGO_APP_ID", "\"1302184408\"")
        buildConfigField("String", "ZEGO_APP_SIGN", "\"0026ffba220be64439be30a172f10dd4566cbfbdd1d560aac0fc283e23149799\"")

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Cho phép android.util.Log và các Android SDK stub trả default value
            // thay vì throw RuntimeException trong unit tests
            isReturnDefaultValues = true
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.firebase.functions)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.11.0")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-auth") // Cho xác thực
    implementation("com.google.firebase:firebase-firestore") // Cho lưu trữ dữ liệu
    implementation("com.google.firebase:firebase-messaging") // Cho thông báo đẩy
    implementation("com.cloudinary:cloudinary-android:3.1.2") // Cho lưu trữ media (thay Firebase Storage)
    implementation("com.github.bumptech.glide:glide:4.16.0") // Cho tải ảnh
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0") // Cho Glide annotation processing
    // ProcessLifecycleOwner — theo dõi foreground/background của toàn bộ app
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    // ZEGOCLOUD - Gọi thoại và video
    implementation("im.zego:zego_uikit_prebuilt_call_android:+")
}
