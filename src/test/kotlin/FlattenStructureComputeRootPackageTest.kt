package ch.frankel.openrewrite.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.nio.file.Paths
import java.util.Locale.getDefault

class FlattenStructureComputeRootPackageTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FlattenStructure())
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
    }

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

        rewriteRun(
            kotlin(sourceCode1) { spec ->
                spec.path("src/main/kotlin/ch/frankel/blog/foo/Foo.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/foo/Foo.kt"),
                        it.sourcePath
                    )
                }
            },
            kotlin(sourceCode2) { spec ->
                spec.path("src/main/kotlin/org/frankel/blog/bar/Bar.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/bar/Bar.kt"),
                        it.sourcePath
                    )
                }
            },
        )
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

        sourceCodes.mapIndexed { i, it ->
            kotlin(it) { spec ->
                spec.path("src/main/kotlin/${rootPackage.replace('.', '/')}/${capitalize(subPackages[i])}.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/${subPackages[i]}/${capitalize(subPackages[i])}.kt"),
                        it.sourcePath
                    )
                }
            }
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

        rewriteRun(
            kotlin(sourceCode1) { spec ->
                spec.path("src/main/kotlin/ch/frankel/blog/Foo.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/Foo.kt"),
                        it.sourcePath
                    )
                }
            },
            kotlin(sourceCode2) { spec ->
                spec.path("src/main/kotlin/ch/frankel/blog/bar/Bar.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/bar/Bar.kt"),
                        it.sourcePath
                    )
                }
            },
        )
    }

    @Test
    fun `should not flatten disjoint packages `() {
        val sourceCode1 = """
            package ch.frankel.blog.foo
            
            class Foo
        """
        val sourceCode2 = """
            package org.frankel.blog.bar
            
            class Bar
        """

        rewriteRun(
            { spec ->
                spec.recipe(FlattenStructure())
                    .cycles(1)
                    .expectedCyclesThatMakeChanges(0)
            },
            kotlin(sourceCode1) { spec ->
                spec.path("src/main/kotlin/ch/frankel/blog/foo/Foo.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/ch/frankel/blog/foo/Foo.kt"),
                        it.sourcePath
                    )
                }
            },
            kotlin(sourceCode2) { spec ->
                spec.path("src/main/kotlin/org/frankel/blog/bar/Bar.kt")
                spec.afterRecipe {
                    assertEquals(
                        Paths.get("src/main/kotlin/org/frankel/blog/bar/Bar.kt"),
                        it.sourcePath
                    )
                }
            },
        )
    }
}
