package ch.frankel.openrewrite.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import java.nio.file.Paths
import java.util.Locale.getDefault
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import kotlin.streams.toList

class FlattenStructureComputeRootPackageTest {

    @Test
    fun `should flatten ch_frankel_blog_foo and ch_frankel_blog_bar to foo and bar`() {
        val sourceCode1 = """
            package ch.frankel.blog.foo
            
            class Foo
        """
        val sourceCode2 = """
            package ch.frankel.blog.bar
            
            class Bar
        """

        // Given
        val parser = KotlinParser.builder().build()
        val cus = parser.parse(
            InMemoryExecutionContext(),
            sourceCode1, sourceCode2
        ).collect(Collectors.toList())
        val modifiedCu1 =
            (cus[0] as K.CompilationUnit).withSourcePath(Paths.get("src/main/kotlin/ch/frankel/blog/foo/Foo.kt"))
        val modifiedCu2 =
            (cus[1] as K.CompilationUnit).withSourcePath(Paths.get("src/main/kotlin/ch/frankel/blog/bar/Bar.kt"))

        // When
        val recipe = FlattenStructure()
        val context = InMemoryExecutionContext()
        val acc = AtomicReference<String?>(null)
        recipe.getScanner(acc).visit(modifiedCu1, context)
        recipe.getScanner(acc).visit(modifiedCu2, context)
        val result1 = recipe.getVisitor(acc).visit(modifiedCu1, context)
        val result2 = recipe.getVisitor(acc).visit(modifiedCu2, context)

        // Then
        assertEquals(Paths.get("src/main/kotlin/foo/Foo.kt"), (result1 as SourceFile).sourcePath)
        assertEquals(Paths.get("src/main/kotlin/bar/Bar.kt"), (result2 as SourceFile).sourcePath)
    }

    @Test
    fun `should flatten many packages`() {

        fun capitalize(s: String): String = s.replaceFirstChar { it.titlecase(getDefault()) }

        val rootPackage = "ch.frankel.blog"
        val subPackages = listOf("foo", "bar", "baz", "qux", "quux")
        val sourceCodes: List<String> = subPackages.map {
            """
            package $rootPackage.$it
            
            class ${capitalize(it)}
            """
        }

        // Given
        val parser = KotlinParser.builder().build()
        val cus = parser.parse(
            InMemoryExecutionContext(),
            *sourceCodes.toTypedArray()
        ).map { it as K.CompilationUnit  }
        .toList<K.CompilationUnit>()
        .mapIndexed { i, it -> it.withSourcePath(Paths.get("src/main/kotlin/${subPackages[i]}/${capitalize(subPackages[i])}.kt")) }

        // When
        val recipe = FlattenStructure()
        val context = InMemoryExecutionContext()
        val acc = AtomicReference<String?>(null)
        cus.forEach {
            recipe.getScanner(acc).visit(it, context)
        }
        cus.forEach {
            recipe.getVisitor(acc).visit(it, context)
        }

        // Then
        cus.forEachIndexed { index, it ->
            assertEquals("src/main/kotlin/${subPackages[index]}/${capitalize(subPackages[index])}.kt", it.sourcePath.toString())
        }

    }

    @Test
    fun `should flatten ch_frankel_blog and ch_frankel_blog_bar to root and bar`() {
        val sourceCode1 = """
            package ch.frankel.blog
            
            class Foo
        """
        val sourceCode2 = """
            package ch.frankel.blog.bar
            
            class Bar
        """

        // Given
        val parser = KotlinParser.builder().build()
        val cus = parser.parse(
            InMemoryExecutionContext(),
            sourceCode1, sourceCode2
        ).collect(Collectors.toList())
        val modifiedCu1 =
            (cus[0] as K.CompilationUnit).withSourcePath(Paths.get("src/main/kotlin/ch/frankel/blog/Foo.kt"))
        val modifiedCu2 =
            (cus[1] as K.CompilationUnit).withSourcePath(Paths.get("src/main/kotlin/ch/frankel/blog/bar/Bar.kt"))

        // When
        val recipe = FlattenStructure()
        val context = InMemoryExecutionContext()
        val acc = AtomicReference<String?>(null)
        recipe.getScanner(acc).visit(modifiedCu1, context)
        recipe.getScanner(acc).visit(modifiedCu2, context)
        val result1 = recipe.getVisitor(acc).visit(modifiedCu1, context)
        val result2 = recipe.getVisitor(acc).visit(modifiedCu2, context)

        // Then
        assertEquals(Paths.get("src/main/kotlin/Foo.kt"), (result1 as SourceFile).sourcePath)
        assertEquals(Paths.get("src/main/kotlin/bar/Bar.kt"), (result2 as SourceFile).sourcePath)
    }

    @Test
    fun `should not flatten disjoint packages`() {
        val sourceCode1 = """
            package ch.frankel.blog.foo
            
            class Foo
        """
        val sourceCode2 = """
            package org.frankel.blog.bar
            
            class Bar
        """
        // Given
        val parser = KotlinParser.builder().build()
        val cus = parser.parse(
            InMemoryExecutionContext(),
            sourceCode1, sourceCode2
        ).collect(Collectors.toList())
        val modifiedCu1 =
            (cus[0] as K.CompilationUnit).withSourcePath(Paths.get("src/main/kotlin/ch/frankel/blog/foo/Foo.kt"))
        val modifiedCu2 =
            (cus[1] as K.CompilationUnit).withSourcePath(Paths.get("src/main/kotlin/org/frankel/blog/bar/Bar.kt"))

        // When
        val recipe = FlattenStructure()
        val context = InMemoryExecutionContext()
        val acc = AtomicReference<String?>(null)
        recipe.getScanner(acc).visit(modifiedCu1, context)
        recipe.getScanner(acc).visit(modifiedCu2, context)
        val result1 = recipe.getVisitor(acc).visit(modifiedCu1, context)
        val result2 = recipe.getVisitor(acc).visit(modifiedCu2, context)

        // Then
        assertEquals(Paths.get("src/main/kotlin/ch/frankel/blog/foo/Foo.kt"), (result1 as SourceFile).sourcePath)
        assertEquals(Paths.get("src/main/kotlin/org/frankel/blog/bar/Bar.kt"), (result2 as SourceFile).sourcePath)

    }
}
