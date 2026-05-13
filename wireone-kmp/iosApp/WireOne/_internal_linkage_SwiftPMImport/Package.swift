// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "_internal_linkage_SwiftPMImport",
    platforms: [
        .iOS("15.0")
    ],
    products: [
        .library(
            name: "_internal_linkage_SwiftPMImport",
            type: .none,
            targets: ["_internal_linkage_SwiftPMImport"]
        )
    ],
    dependencies: [
        .package(
            url: "https://github.com/skiptools/swift-sqlcipher.git",
            exact: "1.9.0"
        )
    ],
    targets: [
        .target(
            name: "_internal_linkage_SwiftPMImport",
            dependencies: [
                .product(
                    name: "SQLCipher",
                    package: "swift-sqlcipher",
                    condition: .when(platforms: [.iOS])
                )
            ]
        )
    ]
)
