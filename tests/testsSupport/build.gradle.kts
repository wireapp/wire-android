import java.util.Properties

plugins {
    id(libs.plugins.wire.android.test.library.get().pluginId)
}
val envPropsFile = project.file("env.properties")
val env = Properties()

if (envPropsFile.exists()) {
    envPropsFile.inputStream().use { env.load(it) }
}
android {
    namespace = "com.wire.android.testSupport"
    defaultConfig {
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "BACKEND_URL" to (env["BACKEND_URL"]?.toString() ?: ""),
                "INBUCKET_USERNAME" to (env["INBUCKET_USERNAME"]?.toString() ?: ""),
                "INBUCKET_PASSWORD" to (env["INBUCKET_PASSWORD"]?.toString() ?: ""),
                "INBUCKET_URL" to (env["INBUCKET_URL"]?.toString() ?: "")
            )
        )
        buildConfigField("String", "BACKEND_URL", "\"${env["BACKEND_URL"] ?: ""}\"")
        buildConfigField("String", "INBUCKET_URL", "\"${env["INBUCKET_URL"] ?: ""}\"")
        buildConfigField("String", "INBUCKET_USERNAME", "\"${env["INBUCKET_USERNAME"] ?: ""}\"")
        buildConfigField("String", "INBUCKET_PASSWORD", "\"${env["INBUCKET_PASSWORD"] ?: ""}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}
android.buildFeatures.buildConfig = true

dependencies {
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.uiAutomator)
    implementation("net.datafaker:datafaker:2.4.2")

}
