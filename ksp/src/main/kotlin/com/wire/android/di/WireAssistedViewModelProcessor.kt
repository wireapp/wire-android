/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.di

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import com.google.devtools.ksp.validate

private const val BINDING_ANNOTATION = "com.wire.android.di.metro.WireAssistedViewModelBinding"
private const val GROUP_ANNOTATION = "com.wire.android.di.metro.WireAssistedViewModelFactoryGroup"
private const val ASSISTED_FACTORY_ANNOTATION = "dev.zacsweers.metro.AssistedFactory"

class WireAssistedViewModelProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        WireAssistedViewModelProcessor(environment.codeGenerator, environment.logger)
}

internal class WireAssistedViewModelProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val generatedGroups = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(BINDING_ANNOTATION).toList()
        val deferred = symbols.filterNot(KSAnnotated::validate)
        val bindings = symbols.filterIsInstance<KSClassDeclaration>().filter(KSAnnotated::validate)

        bindings.mapNotNull(::parseBinding)
            .groupBy(AssistedBinding::groupQualifiedName)
            .forEach { (groupName, groupBindings) ->
                if (generatedGroups.add(groupName)) generateGroup(groupBindings)
            }
        return deferred
    }

    private fun parseBinding(viewModel: KSClassDeclaration): AssistedBinding? = runCatching {
        require(viewModel.classKind == ClassKind.CLASS) {
            "@$BINDING_ANNOTATION can only be applied to classes"
        }
        val annotation = viewModel.annotation(BINDING_ANNOTATION)
        val groupType = annotation.typeArgument("group")
        val group = groupType.declaration as? KSClassDeclaration
            ?: error("group must reference a class or object")
        require(group.classKind == ClassKind.OBJECT) { "group must reference an object" }
        val groupAnnotation = group.annotation(GROUP_ANNOTATION)
        val factoryName = groupAnnotation.stringArgument("factoryName")
            .ifBlank { group.simpleName.asString().removeSuffix("Group") }
        require(factoryName.isNotBlank() && factoryName.isValidIdentifier()) {
            "factoryName must be a valid Kotlin identifier"
        }
        val factoryMethod = annotation.stringArgument("factoryMethod")
            .ifBlank { viewModel.simpleName.asString().defaultFactoryMethod() }
        require(factoryMethod.isValidIdentifier()) { "factoryMethod must be a valid Kotlin identifier" }

        val factories = viewModel.declarations.filterIsInstance<KSClassDeclaration>()
            .filter { it.hasAnnotation(ASSISTED_FACTORY_ANNOTATION) }
            .toList()
        require(factories.size == 1) {
            "${viewModel.qualifiedName?.asString()} must declare exactly one nested @AssistedFactory"
        }
        val factory = factories.single()
        val createMethods = factory.declarations.filterIsInstance<KSFunctionDeclaration>()
            .filter { it.simpleName.asString() == "create" }
            .toList()
        require(createMethods.size == 1) {
            "${factory.qualifiedName?.asString()} must declare exactly one create method"
        }
        val create = createMethods.single()
        val returnType = requireNotNull(create.returnType?.resolve()) { "create must declare a return type" }
        AssistedBinding(
            groupQualifiedName = requireNotNull(group.qualifiedName?.asString()),
            groupPackage = group.packageName.asString(),
            factoryName = factoryName,
            isInternal = Modifier.INTERNAL in group.modifiers,
            factoryMethod = factoryMethod,
            metroFactoryType = requireNotNull(factory.qualifiedName?.asString()),
            returnType = returnType.render(),
            parameters = create.parameters.mapIndexed { index, parameter ->
                AssistedParameter(
                    name = parameter.name?.asString() ?: "arg$index",
                    type = parameter.type.resolve().render(),
                )
            },
            files = listOfNotNull(viewModel.containingFile, group.containingFile),
        )
    }.getOrElse { failure ->
        logger.error(failure.message ?: "Invalid assisted ViewModel binding", viewModel)
        null
    }

    @Suppress("SpreadOperator")
    private fun generateGroup(bindings: List<AssistedBinding>) {
        val first = bindings.first()
        require(bindings.map(AssistedBinding::factoryName).distinct().size == 1) {
            "All bindings in ${first.groupQualifiedName} must use the same generated factory name"
        }
        require(bindings.map(AssistedBinding::factoryMethod).distinct().size == bindings.size) {
            "Factory methods in ${first.groupQualifiedName} must be unique"
        }
        require(bindings.map(AssistedBinding::isInternal).distinct().size == 1) {
            "All bindings in ${first.groupQualifiedName} must use the same visibility"
        }
        val content = WireAssistedViewModelRenderer.render(first.groupPackage, first.factoryName, bindings)
        val dependencies = Dependencies(
            aggregating = true,
            *bindings.flatMap(AssistedBinding::files).distinct().toTypedArray(),
        )
        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = first.groupPackage,
            fileName = "${first.factoryName}Generated",
            extensionName = "kt",
        ).use { it.write(content.toByteArray()) }
    }
}

internal object WireAssistedViewModelRenderer {
    fun render(packageName: String, factoryName: String, bindings: List<AssistedBinding>): String = buildString {
        val sortedBindings = bindings.sortedBy(AssistedBinding::factoryMethod)
        val visibility = if (bindings.first().isInternal) "internal " else ""
        appendLine("package $packageName")
        appendLine()
        appendLine("import dev.zacsweers.metro.BindingContainer")
        appendLine("import dev.zacsweers.metro.IntoMap")
        appendLine("import dev.zacsweers.metro.Provides")
        appendLine("import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory")
        appendLine("import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey")
        appendLine()
        appendLine("${visibility}interface $factoryName : ManualViewModelAssistedFactory {")
        sortedBindings.forEach { binding ->
            appendLine("    fun ${binding.factoryMethod}(${binding.parameters.renderDeclaration()}): ${binding.returnType}")
        }
        appendLine("}")
        appendLine()
        appendLine("@BindingContainer")
        appendLine("object ${factoryName}MetroBindings {")
        appendLine("    @Provides")
        appendLine("    @IntoMap")
        appendLine("    @ManualViewModelAssistedFactoryKey($factoryName::class)")
        appendLine("    ${visibility}fun bind(")
        sortedBindings.forEachIndexed { index, binding ->
            appendLine("        factory$index: ${binding.metroFactoryType},")
        }
        appendLine("    ): ManualViewModelAssistedFactory =")
        appendLine("        object : $factoryName {")
        sortedBindings.forEachIndexed { index, binding ->
            appendLine(
                "            override fun ${binding.factoryMethod}(${binding.parameters.renderDeclaration()}): " +
                    "${binding.returnType} = factory$index.create(${binding.parameters.joinToString { it.name }})"
            )
        }
        appendLine("        }")
        appendLine("}")
    }

    private fun List<AssistedParameter>.renderDeclaration(): String = joinToString { "${it.name}: ${it.type}" }
}

internal data class AssistedBinding(
    val groupQualifiedName: String,
    val groupPackage: String,
    val factoryName: String,
    val factoryMethod: String,
    val metroFactoryType: String,
    val returnType: String,
    val parameters: List<AssistedParameter>,
    val isInternal: Boolean = false,
    val files: List<com.google.devtools.ksp.symbol.KSFile> = emptyList(),
)

internal data class AssistedParameter(val name: String, val type: String)

private fun KSDeclaration.hasAnnotation(name: String): Boolean = annotations.any {
    it.annotationType.resolve().declaration.qualifiedName?.asString() == name
}

private fun KSDeclaration.annotation(name: String): KSAnnotation = annotations.singleOrNull {
    it.annotationType.resolve().declaration.qualifiedName?.asString() == name
} ?: error("${qualifiedName?.asString()} must be annotated with @$name")

private fun KSAnnotation.stringArgument(name: String): String =
    arguments.single { it.name?.asString() == name }.value as String

private fun KSAnnotation.typeArgument(name: String): KSType =
    arguments.single { it.name?.asString() == name }.value as KSType

private fun String.isValidIdentifier(): Boolean = matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))

internal fun String.defaultFactoryMethod(): String = removeSuffix("Impl").replaceFirstChar(Char::lowercaseChar)

private fun KSType.render(): String {
    val declarationName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
    val renderedArguments = arguments.takeIf(List<KSTypeArgument>::isNotEmpty)?.joinToString(
        prefix = "<",
        postfix = ">",
    ) { argument ->
        val type = argument.type?.resolve()?.render() ?: "*"
        when (argument.variance) {
            Variance.COVARIANT -> "out $type"
            Variance.CONTRAVARIANT -> "in $type"
            else -> type
        }
    }.orEmpty()
    val nullable = if (nullability == Nullability.NULLABLE) "?" else ""
    return declarationName + renderedArguments + nullable
}
