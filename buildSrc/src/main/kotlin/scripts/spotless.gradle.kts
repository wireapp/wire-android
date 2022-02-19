package scripts

apply(plugin = "com.diffplug.spotless")

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        ktlint().userData(
            mapOf(
                "disabled_rules" to "import-ordering",
                "max_line_length" to "150"
            )
        )
        trimTrailingWhitespace()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    format("xml") {
        target("**/*.xml")
        trimTrailingWhitespace()
    }
}
