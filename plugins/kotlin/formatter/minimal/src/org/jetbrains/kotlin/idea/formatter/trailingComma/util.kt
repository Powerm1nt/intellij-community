// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

@file:OptIn(UnsafeCastFunction::class)

package org.jetbrains.kotlin.idea.formatter.trailingComma

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast

fun trailingCommaIsAllowedOnCallSite(): Boolean = Registry.`is`("kotlin.formatter.allowTrailingCommaOnCallSite")

private val TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE = TokenSet.create(
    KtNodeTypes.TYPE_PARAMETER_LIST,
    KtNodeTypes.DESTRUCTURING_DECLARATION,
    KtNodeTypes.WHEN_ENTRY,
    KtNodeTypes.FUNCTION_LITERAL,
    KtNodeTypes.VALUE_PARAMETER_LIST,
    KtNodeTypes.CONTEXT_RECEIVER_LIST,
)

private val TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE = TokenSet.create(
    KtNodeTypes.COLLECTION_LITERAL_EXPRESSION,
    KtNodeTypes.TYPE_ARGUMENT_LIST,
    KtNodeTypes.INDICES,
    KtNodeTypes.VALUE_ARGUMENT_LIST,
)

private val TYPES_WITH_TRAILING_COMMA = TokenSet.orSet(
    TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE,
    TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE,
)

fun PsiElement.canAddTrailingCommaWithRegistryCheck(): Boolean {
    val type = PsiUtilCore.getElementType(this) ?: return false
    return type in TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE ||
            trailingCommaIsAllowedOnCallSite() && type in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE
}

fun KotlinCodeStyleSettings.addTrailingCommaIsAllowedFor(node: ASTNode): Boolean =
    addTrailingCommaIsAllowedFor(PsiUtilCore.getElementType(node))

fun KotlinCodeStyleSettings.addTrailingCommaIsAllowedFor(element: PsiElement): Boolean =
    addTrailingCommaIsAllowedFor(PsiUtilCore.getElementType(element))

private fun KotlinCodeStyleSettings.isTrailingCommaAllowedFor(type: IElementType?) = when (type) {
    KtNodeTypes.WHEN_ENTRY -> ALLOW_TRAILING_COMMA_WHEN_ENTRY
    KtNodeTypes.TYPE_PARAMETER_LIST -> ALLOW_TRAILING_COMMA_TYPE_PARAMETER_LIST
    KtNodeTypes.DESTRUCTURING_DECLARATION -> ALLOW_TRAILING_COMMA_DESTRUCTURING_DECLARATION
    KtNodeTypes.FUNCTION_LITERAL -> ALLOW_TRAILING_COMMA_FUNCTION_LITERAL
    KtNodeTypes.VALUE_PARAMETER_LIST -> ALLOW_TRAILING_COMMA_VALUE_PARAMETER_LIST
    KtNodeTypes.CONTEXT_RECEIVER_LIST -> ALLOW_TRAILING_COMMA_CONTEXT_RECEIVER_LIST
    KtNodeTypes.COLLECTION_LITERAL_EXPRESSION -> ALLOW_TRAILING_COMMA_COLLECTION_LITERAL_EXPRESSION
    KtNodeTypes.TYPE_ARGUMENT_LIST -> ALLOW_TRAILING_COMMA_TYPE_ARGUMENT_LIST
    KtNodeTypes.INDICES -> ALLOW_TRAILING_COMMA_INDICES
    KtNodeTypes.VALUE_ARGUMENT_LIST -> ALLOW_TRAILING_COMMA_VALUE_ARGUMENT_LIST
    else -> true
}

private fun KotlinCodeStyleSettings.addTrailingCommaIsAllowedFor(type: IElementType?): Boolean {
    if (type == null) return false
    if (type in TYPES_WITH_TRAILING_COMMA && ALLOW_TRAILING_COMMA && isTrailingCommaAllowedFor(type)) return true
    return type in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE && (ALLOW_TRAILING_COMMA_ON_CALL_SITE || trailingCommaIsAllowedOnCallSite())
}

fun PsiElement.canAddTrailingComma(): Boolean = when {
    this is KtWhenEntry && (isElse || parent.cast<KtWhenExpression>().leftParenthesis == null || guard != null) -> false
    this is KtFunctionLiteral && arrow == null -> false
    else -> PsiUtilCore.getElementType(this) in TYPES_WITH_TRAILING_COMMA
}