import scripts.Variants_gradle.BuildTypes

/**
 * Config fields with DEFAULT values per Build Type.
 */
enum class ConfigFields(val defaultValue: String) {
    SUPPORT_URL(""""https://support.wire.com""""),
    SENDER_ID(""""782078216207"""")
}

/**
 * Defines a map for fields per Build Type.
 */
object ClientConfig {
    val properties = mapOf(

        // Config field values for DEBUG Build Type
        BuildTypes.DEBUG to ConfigFields.values().associate { Pair(it, it.defaultValue) },

        // Config field values for RELEASE Build Type
        // TODO: Certificate pinning, change backend based on flavour
        BuildTypes.RELEASE to mapOf(
            ConfigFields.SUPPORT_URL to """"https://support.wire.com"""",
            ConfigFields.SENDER_ID to """"782078216207""""
        )
    )
}
