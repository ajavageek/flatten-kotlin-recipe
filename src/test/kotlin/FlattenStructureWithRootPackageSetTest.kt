package ch.frankel.openrewrite.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RewriteTest
import java.nio.file.Paths
import java.util.stream.Stream

class FlattenStructureWithRootPackageSetTest : RewriteTest {

    @ParameterizedTest
    @MethodSource("testData")
    fun `should flatten accordingly`(
        sourceCode: String,
        originalPath: String,
        configuredRootPackage: String,
        expectedPath: String,
        cycles: Int
    ) {

        rewriteRun(
            { spec ->
                spec.recipe(FlattenStructure(configuredRootPackage))
                    .cycles(cycles)
                    .expectedCyclesThatMakeChanges(cycles)
            },
            kotlin(sourceCode) { spec ->
                spec.path(originalPath)
                spec.afterRecipe {
                    assertEquals(
                        Paths.get(expectedPath),
                        it.sourcePath
                    )
                }
            })
    }

    companion object {

        private val nominalCase = Arguments.of(
            """
            package com.example.demo

            class Test
            """, "src/main/kotlin/com/example/demo/Test.kt", "com.example.demo", "src/main/kotlin/Test.kt", 1
        )

        private val sourceNotUnderRoot = Arguments.of(
            """
            package org.other

            class Test
            """, "src/main/kotlin/org/other/Test.kt", "com.example.demo", "src/main/kotlin/org/other/Test.kt", 0
        )

        private val deeplyNestedSource = Arguments.of(
            """
                package com.example.deep.nested.structure

                class Test
                """,
            "src/main/kotlin/com/example/deep/nested/structure/Test.kt",
            "com.example",
            "src/main/kotlin/deep/nested/structure/Test.kt", 1
        )

        @JvmStatic
        private fun testData() = Stream.of(nominalCase, sourceNotUnderRoot, deeplyNestedSource)
    }
}
