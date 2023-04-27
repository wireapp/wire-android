package customization

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import flavor.ProductFlavors
import java.io.File

fun BaseExtension.overrideResourcesForAllFlavors(
    customResourcesRootDir: File
) {

    sourceSets {
        ProductFlavors.all.forEach {
            getByName(it.buildName).apply {
                val resDir = (res as DefaultAndroidSourceDirectorySet).srcDirs.first()
                println("Copying files from '${customResourcesRootDir.absolutePath}' into '${resDir.absolutePath}'")

                customResourcesRootDir.walkTopDown().filter { !it.isDirectory }.forEach { customContent ->
                    val relativePath = customContent.relativeTo(customResourcesRootDir).path
                    val targetFile = File(resDir, relativePath)
                    customContent.copyTo(targetFile, overwrite = true)
                }
            }
        }
    }
}
