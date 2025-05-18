package ch.frankel.openrewrite.kotlin

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.kotlin.KotlinIsoVisitor
import org.openrewrite.kotlin.tree.K
import java.nio.file.Path
import java.nio.file.Paths

class FlattenStructure(private val rootPackage: String) : Recipe() {

    override fun getDisplayName(): String = "Flatten Kotlin package directory structure"
    override fun getDescription(): String =
        "Move Kotlin files to match idiomatic layout by omitting the root package according to the official recommendation."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        return object : KotlinIsoVisitor<ExecutionContext>() {
            override fun visitCompilationUnit(cu: K.CompilationUnit, ctx: ExecutionContext): K.CompilationUnit {
                val packageName = cu.packageDeclaration?.packageName ?: return cu
                if (!packageName.startsWith(rootPackage)) return cu
                val relativePath = packageName.removePrefix(rootPackage).removePrefix(".")
                    .replace('.', '/')
                val filename = cu.sourcePath.fileName.toString()
                val newPath: Path = Paths.get("src/main/kotlin")
                    .resolve(relativePath)
                    .resolve(filename)
                return cu.withSourcePath(newPath)
            }
        }
    }
}
