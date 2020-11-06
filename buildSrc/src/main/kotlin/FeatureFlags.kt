import scripts.Variants_gradle.ProductFlavors

/**
 * By convention use the prefix FEATURE_ for every
 * defined functionality that will be under a feature flag.
 */
enum class Features {
    FEATURE_SEARCH,
    FEATURE_CONVERSATIONS
}

/**
 * Defines a map for activated flags per product flavor.
 */
object FeatureFlags {
    val activated = mapOf(

        //Enabled Features for DEV Product Flavor
        ProductFlavors.DEV to setOf(
            Features.FEATURE_SEARCH,
            Features.FEATURE_CONVERSATIONS),

        //Enabled Features for INTERNAL Product Flavor
        ProductFlavors.INTERNAL to setOf(
            Features.FEATURE_CONVERSATIONS),

        //Enabled Features for PUBLIC Product Flavor
        ProductFlavors.PUBLIC to setOf(
            Features.FEATURE_SEARCH))
}
