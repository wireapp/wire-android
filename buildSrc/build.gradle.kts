repositories {
    jcenter()
    google()
}

plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}