apply(plugin = "com.android.application")
apply(plugin = "com.google.gms.google-services")
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
//    id("com.google.devtools.ksp")
//    id("kotlin-parcelize")
}

android {
    namespace = "com.sysu.edu"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sysu.edu"
        minSdk = 26
        targetSdk = 36
        versionCode = 1935
        versionName = "1.1.3beta2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
    buildFeatures {
        viewBinding = true
        compose = true
        aidl = true
    }
    sourceSets {
        getByName("main") {
            java {
                mutableSetOf(
                    "src\\main\\java"
                )
            }
        }
    }
}

dependencies {

    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.fastjson2)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.legacy.support.v4)
    implementation(libs.activity)
    implementation(libs.annotation)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.preference)
    implementation(libs.work.runtime)
    implementation(libs.material.preference)
    {
        exclude("dev.rikka.rikkax.appcompat", "appcompat")
    }
    implementation(libs.dev.material)
    {
        exclude("dev.rikka.rikkax.appcompat", "appcompat")
    }
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.core)
    implementation(libs.ext.tables)
    implementation(libs.ext.strikethrough)
    implementation(libs.google.material)
    implementation(libs.recycler)
    implementation(libs.recycler.table)
    implementation(libs.inline.parser)
//    implementation(libs.core.remoteviews)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.androidx.fragment)
//    implementation(libs.enro.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    implementation(libs.api)
    implementation(libs.provider)
    implementation(libs.html)
    implementation(platform(libs.editor.bom))
    implementation(libs.editor)
    implementation(libs.language.textmate)
    implementation(project(":CalendarView"))
    implementation(libs.okhttp.java.net.cookiejar)
//    implementation(libs.enro)
//    ksp(libs.enro.processor) // both kapt and ksp are supported
//    testImplementation(libs.enro.test)
//    implementation(libs.dev.enro)
    /*configurations.all {
        exclude("androidx.appcompat", "appcompat")
    }*/
    //api(libs.wechat.sdk.android)
}