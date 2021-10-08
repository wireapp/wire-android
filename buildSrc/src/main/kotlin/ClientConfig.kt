import scripts.Variants_gradle.BuildTypes

/**
 * Config fields with DEFAULT values per Build Type.
 */
enum class ConfigFields(val defaultValue: String) {
    API_BASE_URL(""""https://staging-nginz-https.zinfra.io""""),
    ACCOUNTS_URL(""""https://wire-account-staging.zinfra.io""""),
    WEB_SOCKET_URL(""""https://staging-nginz-ssl.zinfra.io/await?client="""")
}

/**
 * Defines a map for fields per Build Type.
 */
object ClientConfig {
    val properties = mapOf(

        //Config field values for DEBUG Build Type
        BuildTypes.DEBUG to mapOf(
            ConfigFields.API_BASE_URL to ConfigFields.API_BASE_URL.defaultValue,
            ConfigFields.ACCOUNTS_URL to ConfigFields.ACCOUNTS_URL.defaultValue,
            ConfigFields.WEB_SOCKET_URL to ConfigFields.WEB_SOCKET_URL.defaultValue
        ),
        //Config field values for RELEASE Build Type
        //TODO: Certificate pinning, change backend based on flavour
        BuildTypes.RELEASE to mapOf(
            ConfigFields.API_BASE_URL to """"https://prod-nginz-https.wire.com"""",
            ConfigFields.ACCOUNTS_URL to """"https://account.wire.com"""",
            ConfigFields.WEB_SOCKET_URL to """"https://prod-nginz-ssl.wire.com""""
        )
    )
}
