package org.jetbrains.dukat.compiler.lowerings

import org.jetbrains.dukat.ast.model.nodes.DocumentRootNode
import org.jetbrains.dukat.ast.model.nodes.EnumNode
import org.jetbrains.dukat.ast.model.nodes.SourceSetNode
import org.jetbrains.dukat.ast.model.nodes.TypeValueNode
import org.jetbrains.dukat.ast.model.nodes.transform
import org.jetbrains.dukat.astCommon.IdentifierEntity
import org.jetbrains.dukat.astCommon.NameEntity
import org.jetbrains.dukat.astCommon.QualifierEntity
import org.jetbrains.dukat.astCommon.TopLevelEntity
import org.jetbrains.dukat.panic.raiseConcern
import org.jetbrains.dukat.tsmodel.types.ParameterValueDeclaration
import org.jetrbains.dukat.nodeLowering.NodeTypeLowering

private fun escapeIdentificator(identificator: String): String {
    val reservedWords = setOf(
            "as",
            "fun",
            "in",
            "interface",
            "is",
            "object",
            "package",
            "typealias",
            "typeof",
            "val",
            "var",
            "when"
    )

    val isReservedWord = reservedWords.contains(identificator)
    val containsDollarSign = identificator.contains("$")
    val containsOnlyUnderscores = "^_+$".toRegex().containsMatchIn(identificator)
    val isEscapedAlready = "^`.*`$".toRegex().containsMatchIn(identificator)

    return if (!isEscapedAlready && (isReservedWord || containsDollarSign || containsOnlyUnderscores)) {
        "`${identificator}`"
    } else {
        identificator
    }
}


private class EscapeIdentificators : NodeTypeLowering {

    private fun IdentifierEntity.escape(): IdentifierEntity {
        return copy(value = lowerIdentificator(value))
    }

    private fun TypeValueNode.escape(): TypeValueNode {
        return when (val typeNodeValue = value) {
            is IdentifierEntity -> copy(value = typeNodeValue.escape())
            is QualifierEntity -> copy(value = typeNodeValue.escape())
        }
    }

    private fun QualifierEntity.escape(): QualifierEntity {
        return when(val nodeLeft = left) {
            is IdentifierEntity -> QualifierEntity(nodeLeft.escape(), right.escape())
            is QualifierEntity -> nodeLeft.copy(left = nodeLeft.escape(), right = right.escape())
        }
    }

    private fun NameEntity.escape(): NameEntity {
        return when(this) {
            is IdentifierEntity -> escape()
            is QualifierEntity -> escape()
        }
    }

    override fun lowerIdentificator(identificator: String): String {
        return escapeIdentificator(identificator)
    }

    override fun lowerType(declaration: ParameterValueDeclaration): ParameterValueDeclaration {
        return when (declaration) {
            is TypeValueNode -> declaration.escape()
            else -> {
                super.lowerType(declaration)
            }
        }
    }

    override fun lowerTopLevelEntity(declaration: TopLevelEntity): TopLevelEntity {
        return when (declaration) {
            is EnumNode -> declaration.copy(values = declaration.values.map { value -> value.copy(value = escapeIdentificator(value.value)) })
            else -> super.lowerTopLevelEntity(declaration)
        }
    }

    override fun lowerDocumentRoot(documentRoot: DocumentRootNode): DocumentRootNode {
        return documentRoot.copy(
                qualifiedPackageName = documentRoot.qualifiedPackageName.escape(),
                declarations = lowerTopLevelDeclarations(documentRoot.declarations)
        )
    }
}

fun DocumentRootNode.escapeIdentificators(): DocumentRootNode {
    return EscapeIdentificators().lowerDocumentRoot(this)
}

fun SourceSetNode.escapeIdentificators() = transform { it.escapeIdentificators() }