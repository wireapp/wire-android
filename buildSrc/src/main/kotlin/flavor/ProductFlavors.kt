package flavor

object FlavorDimensions {
    const val DEFAULT = "default"
}

sealed class ProductFlavors(
    val buildName: String,
    val appName: String,
    val dimensions: String = FlavorDimensions.DEFAULT,
    val shareduserId: String = ""
) {
    override fun toString(): String = this.buildName

    object Dev : ProductFlavors("dev", "Wire Dev")
    object Staging : ProductFlavors("staging", "Wire Staging")

    object Beta : ProductFlavors("beta", "Wire Beta")
    object Internal : ProductFlavors("internal", "Wire Internal")
    object Production : ProductFlavors("prod", "Wire", shareduserId = "com.waz.userid")
    object Fdroid : ProductFlavors("fdroid", "Wire", shareduserId = "com.waz.userid")

    companion object {
        val all: Collection<ProductFlavors> = setOf(
            Dev,
            Staging,
            Beta,
            Internal,
            Production,
            Fdroid,
        )
    }
}
