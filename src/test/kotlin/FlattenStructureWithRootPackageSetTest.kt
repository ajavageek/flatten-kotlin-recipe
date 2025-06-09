package ch.frankel.openrewrite.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import java.nio.file.Paths
import java.util.stream.Stream

class FlattenStructureWithRootPackageSetTest {

    @ParameterizedTest
    @MethodSource("testData")
    fun `should flatten accordingly`(
        sourceCode: String,
        originalPath: String,
        configuredRootPackage: String,
        expectedPath: String
    ) {
        // Given
        val parser = KotlinParser.builder().build()
        val cu = parser.parse(
            InMemoryExecutionContext(),
            sourceCode
        ).findFirst()
            .orElseThrow { IllegalStateException("Failed to parse Kotlin file") }
        val originalPath = Paths.get(originalPath)
        val modifiedCu = (cu as K.CompilationUnit).withSourcePath(originalPath)

        // When
        val recipe = FlattenStructure(configuredRootPackage)
        val result = recipe.visitor.visit(modifiedCu, InMemoryExecutionContext())

        // Then
        val expectedPath = Paths.get(expectedPath)
        assertEquals(expectedPath, (result as SourceFile).sourcePath)
    }

    companion object {

        private val nominalCase = Arguments.of(
            """
            package com.example.demo

            class Test
            """, "src/main/kotlin/com/example/demo/Test.kt", "com.example.demo", "src/main/kotlin/Test.kt"
        )

        private val sourceNotUnderRoot = Arguments.of(
            """
            package org.other

            class Test
            """, "src/main/kotlin/org/other/Test.kt", "com.example.demo", "src/main/kotlin/org/other/Test.kt"
        )

        private val deeplyNestedSource = Arguments.of(
            """
                package com.example.deep.nested.structure

                class Test
                """,
            "src/main/kotlin/com/example/deep/nested/structure/Test.kt",
            "com.example",
            "src/main/kotlin/deep/nested/structure/Test.kt"
        )

        @JvmStatic
        private fun testData() = Stream.of(nominalCase, sourceNotUnderRoot, deeplyNestedSource)
    }
}
