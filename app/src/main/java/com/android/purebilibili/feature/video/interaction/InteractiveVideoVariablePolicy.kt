package com.android.purebilibili.feature.video.interaction

private val NATIVE_ACTION_ASSIGN_REGEX = Regex(
    """^\$([A-Za-z0-9_]+)\s*=\s*(.+)$"""
)

private enum class InteractiveTokenType {
    NUMBER,
    VARIABLE,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    LPAREN,
    RPAREN,
    GE,
    LE,
    GT,
    LT,
    EQ,
    NEQ,
    AND,
    OR,
    EOF
}

private data class InteractiveToken(
    val type: InteractiveTokenType,
    val text: String,
    val numberValue: Double? = null
)

internal fun evaluateInteractiveChoiceCondition(
    condition: String,
    variables: Map<String, Double>
): Boolean {
    val trimmed = condition.trim()
    if (trimmed.isEmpty()) return true
    if (trimmed.equals("true", ignoreCase = true)) return true
    if (trimmed.equals("false", ignoreCase = true)) return false

    val tokens = tokenizeInteractiveExpression(trimmed) ?: return true
    val parser = InteractiveExpressionParser(tokens, variables)
    val result = parser.parseBooleanExpression()
    return result ?: true
}

internal fun applyInteractiveNativeAction(
    nativeAction: String,
    variables: MutableMap<String, Double>
) {
    val statements = nativeAction.split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (statements.isEmpty()) return

    statements.forEach { statement ->
        val match = NATIVE_ACTION_ASSIGN_REGEX.matchEntire(statement) ?: return@forEach
        val targetName = match.groupValues[1]
        val expression = match.groupValues[2]
        val value = evaluateInteractiveArithmeticExpression(expression, variables) ?: return@forEach
        variables[targetName] = value
    }
}

private fun evaluateInteractiveArithmeticExpression(
    expression: String,
    variables: Map<String, Double>
): Double? {
    val tokens = tokenizeInteractiveExpression(expression) ?: return null
    val parser = InteractiveExpressionParser(tokens, variables)
    return parser.parseArithmeticExpression()
}

private fun tokenizeInteractiveExpression(input: String): List<InteractiveToken>? {
    val tokens = mutableListOf<InteractiveToken>()
    var index = 0

    while (index < input.length) {
        val ch = input[index]
        when {
            ch.isWhitespace() -> {
                index += 1
            }
            index + 1 < input.length && input.startsWith("&&", index) -> {
                tokens.add(InteractiveToken(InteractiveTokenType.AND, "&&"))
                index += 2
            }
            index + 1 < input.length && input.startsWith("||", index) -> {
                tokens.add(InteractiveToken(InteractiveTokenType.OR, "||"))
                index += 2
            }
            index + 1 < input.length && input.startsWith(">=", index) -> {
                tokens.add(InteractiveToken(InteractiveTokenType.GE, ">="))
                index += 2
            }
            index + 1 < input.length && input.startsWith("<=", index) -> {
                tokens.add(InteractiveToken(InteractiveTokenType.LE, "<="))
                index += 2
            }
            index + 1 < input.length && input.startsWith("==", index) -> {
                tokens.add(InteractiveToken(InteractiveTokenType.EQ, "=="))
                index += 2
            }
            index + 1 < input.length && input.startsWith("!=", index) -> {
                tokens.add(InteractiveToken(InteractiveTokenType.NEQ, "!="))
                index += 2
            }
            ch == '+' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.PLUS, "+"))
                index += 1
            }
            ch == '-' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.MINUS, "-"))
                index += 1
            }
            ch == '*' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.STAR, "*"))
                index += 1
            }
            ch == '/' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.SLASH, "/"))
                index += 1
            }
            ch == '(' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.LPAREN, "("))
                index += 1
            }
            ch == ')' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.RPAREN, ")"))
                index += 1
            }
            ch == '>' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.GT, ">"))
                index += 1
            }
            ch == '<' -> {
                tokens.add(InteractiveToken(InteractiveTokenType.LT, "<"))
                index += 1
            }
            ch == '$' -> {
                val start = index + 1
                var end = start
                while (end < input.length && isInteractiveVariableChar(input[end])) {
                    end += 1
                }
                if (end <= start) return null
                val variableName = input.substring(start, end)
                tokens.add(InteractiveToken(InteractiveTokenType.VARIABLE, variableName))
                index = end
            }
            ch.isDigit() || ch == '.' -> {
                val start = index
                var end = index
                var dotCount = 0
                while (end < input.length && (input[end].isDigit() || input[end] == '.')) {
                    if (input[end] == '.') dotCount += 1
                    if (dotCount > 1) return null
                    end += 1
                }
                val rawNumber = input.substring(start, end)
                val value = rawNumber.toDoubleOrNull() ?: return null
                tokens.add(
                    InteractiveToken(
                        type = InteractiveTokenType.NUMBER,
                        text = rawNumber,
                        numberValue = value
                    )
                )
                index = end
            }
            else -> {
                return null
            }
        }
    }

    tokens.add(InteractiveToken(InteractiveTokenType.EOF, ""))
    return tokens
}

private fun isInteractiveVariableChar(ch: Char): Boolean {
    return ch.isLetterOrDigit() || ch == '_'
}

private class InteractiveExpressionParser(
    private val tokens: List<InteractiveToken>,
    private val variables: Map<String, Double>
) {
    private var position = 0

    fun parseBooleanExpression(): Boolean? {
        val value = parseOrExpression() ?: return null
        return if (currentType() == InteractiveTokenType.EOF) value else null
    }

    fun parseArithmeticExpression(): Double? {
        val value = parseAddSubtract() ?: return null
        return if (currentType() == InteractiveTokenType.EOF) value else null
    }

    private fun parseOrExpression(): Boolean? {
        var result = parseAndExpression() ?: return null
        while (match(InteractiveTokenType.OR)) {
            val right = parseAndExpression() ?: return null
            result = result || right
        }
        return result
    }

    private fun parseAndExpression(): Boolean? {
        var result = parseBooleanPrimary() ?: return null
        while (match(InteractiveTokenType.AND)) {
            val right = parseBooleanPrimary() ?: return null
            result = result && right
        }
        return result
    }

    private fun parseBooleanPrimary(): Boolean? {
        val comparisonStart = position
        val comparisonResult = parseComparisonFromArithmetic()
        if (comparisonResult != null) {
            return comparisonResult
        }
        position = comparisonStart

        if (match(InteractiveTokenType.LPAREN)) {
            val nested = parseOrExpression() ?: return null
            if (!match(InteractiveTokenType.RPAREN)) return null
            return nested
        }

        return null
    }

    private fun parseComparisonFromArithmetic(): Boolean? {
        val left = parseAddSubtract() ?: return null
        val operator = parseComparisonOperator() ?: return left != 0.0
        val right = parseAddSubtract() ?: return null
        return compare(left, right, operator)
    }

    private fun parseComparisonOperator(): InteractiveTokenType? {
        val type = currentType()
        return when (type) {
            InteractiveTokenType.GE,
            InteractiveTokenType.LE,
            InteractiveTokenType.GT,
            InteractiveTokenType.LT,
            InteractiveTokenType.EQ,
            InteractiveTokenType.NEQ -> {
                position += 1
                type
            }
            else -> null
        }
    }

    private fun compare(left: Double, right: Double, operator: InteractiveTokenType): Boolean {
        return when (operator) {
            InteractiveTokenType.GE -> left >= right
            InteractiveTokenType.LE -> left <= right
            InteractiveTokenType.GT -> left > right
            InteractiveTokenType.LT -> left < right
            InteractiveTokenType.EQ -> left == right
            InteractiveTokenType.NEQ -> left != right
            else -> false
        }
    }

    private fun parseAddSubtract(): Double? {
        var result = parseMultiplyDivide() ?: return null
        while (true) {
            when {
                match(InteractiveTokenType.PLUS) -> {
                    val right = parseMultiplyDivide() ?: return null
                    result += right
                }
                match(InteractiveTokenType.MINUS) -> {
                    val right = parseMultiplyDivide() ?: return null
                    result -= right
                }
                else -> return result
            }
        }
    }

    private fun parseMultiplyDivide(): Double? {
        var result = parseUnary() ?: return null
        while (true) {
            when {
                match(InteractiveTokenType.STAR) -> {
                    val right = parseUnary() ?: return null
                    result *= right
                }
                match(InteractiveTokenType.SLASH) -> {
                    val right = parseUnary() ?: return null
                    if (right == 0.0) return 0.0
                    result /= right
                }
                else -> return result
            }
        }
    }

    private fun parseUnary(): Double? {
        return when {
            match(InteractiveTokenType.PLUS) -> parseUnary()
            match(InteractiveTokenType.MINUS) -> parseUnary()?.unaryMinus()
            else -> parsePrimaryValue()
        }
    }

    private fun parsePrimaryValue(): Double? {
        if (match(InteractiveTokenType.LPAREN)) {
            val nested = parseAddSubtract() ?: return null
            if (!match(InteractiveTokenType.RPAREN)) return null
            return nested
        }

        return when (currentType()) {
            InteractiveTokenType.NUMBER -> {
                val value = currentToken().numberValue
                position += 1
                value
            }
            InteractiveTokenType.VARIABLE -> {
                val name = currentToken().text
                position += 1
                variables[name] ?: 0.0
            }
            else -> null
        }
    }

    private fun match(type: InteractiveTokenType): Boolean {
        if (currentType() != type) return false
        position += 1
        return true
    }

    private fun currentType(): InteractiveTokenType {
        return currentToken().type
    }

    private fun currentToken(): InteractiveToken {
        return tokens.getOrElse(position) { InteractiveToken(InteractiveTokenType.EOF, "") }
    }
}
