import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project

enum class Module {
    CRYPTO;

    fun asDependencyTo(dependencyHandlerScope: DependencyHandlerScope): ProjectDependency {
        return dependencyHandlerScope.project(":" + this.name.toLowerCase())
    }
}
