package ch.frankel.openrewrite.kotlin

import org.openrewrite.ExecutionContext
import org.openrewrite.ScanningRecipe
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.kotlin.KotlinIsoVisitor
import org.openrewrite.kotlin.tree.K
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

class FlattenStructure(private val rootPackage: String?) : ScanningRecipe<AtomicReference<String?>>() {

    constructor() : this(null)

    override fun getDisplayName(): String = "Flatten Kotlin package directory structure"
    override fun getDescription(): String =
        "Move Kotlin files to match idiomatic layout by omitting the root package according to the official recommendation."

    override fun getInitialValue(ctx: ExecutionContext) = AtomicReference<String?>(null)

    override fun getScanner(acc: AtomicReference<String?>): TreeVisitor<*, ExecutionContext> {
        // Root package manually set, skip computation
        if (rootPackage != null) return TreeVisitor.noop<Tree, ExecutionContext>()
        val currentPackage = acc.get()
        // Disjoint packages, skip computation
        if (currentPackage == "") return TreeVisitor.noop<Tree, ExecutionContext>()
        return object : KotlinIsoVisitor<ExecutionContext>() {
            override fun visitCompilationUnit(cu: K.CompilationUnit, ctx: ExecutionContext): K.CompilationUnit {
                val packageName = cu.packageDeclaration?.packageName ?: return cu
                // Different call than the one above!
                val currentPackage = acc.get()
                // First scanned file
                if (currentPackage == null) acc.set(packageName)
                else {
                    // Find the longest common prefix between the stored package and the current one
                    val commonPrefix = packageName.commonPrefixWith(currentPackage).removeSuffix(".")
                    acc.set(commonPrefix)
                }
                return cu
            }
        }
    }

    override fun getVisitor(acc: AtomicReference<String?>): TreeVisitor<*, ExecutionContext> {
        return object : KotlinIsoVisitor<ExecutionContext>() {
            override fun visitCompilationUnit(cu: K.CompilationUnit, ctx: ExecutionContext): K.CompilationUnit {
                val packageName = cu.packageDeclaration?.packageName ?: return cu
                val packageToSet: String? = rootPackage ?: acc.get()
                if (packageToSet == null || packageToSet.isEmpty()) return cu
                val relativePath = packageName.removePrefix(packageToSet).removePrefix(".")
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
