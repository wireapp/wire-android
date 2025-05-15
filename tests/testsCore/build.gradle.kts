plugins {
    id(libs.plugins.wire.android.test.library.get().pluginId)
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    sourceSets {
        getByName("androidTest") {
            kotlin.srcDirs("src/androidTest/kotlin")
            kotlin.srcDirs(project(":tests:testsSupport").file("src/androidTest/kotlin"))
        }
    }
}


dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.uiAutomator)
    androidTestImplementation(project(":tests:testsSupport"))
    implementation("net.datafaker:datafaker:2.4.1")
    implementation("org.apache.commons:commons-text:1.1")
    implementation("org.hamcrest:hamcrest-library:2.2")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.sun.mail:javax.mail:1.5.2")
    implementation("com.stripe:stripe-java:22.16.0")

    implementation("org.glassfish.jersey.core:jersey-client:2.39.1")
}

