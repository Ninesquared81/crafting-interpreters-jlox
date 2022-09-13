package com.craftinginterpreters.lox;

enum ParseRule {
    // Expression rules.
    ASSIGN_EXPR, BINARY_EXPR, CALL_EXPR, CONDITIONAL_EXPR, FUNCTION_EXPR,
    GROUPING_EXPR, LITERAL_EXPR, LOGICAL_EXPR, UNARY_EXPR, VARIABLE_EXPR,
    PRIMARY_EXPR,

    // Statement rules.
    BLOCK_STMT, BREAK_STMT, CONTINUE_STMT, EMPTY_STMT, EXPRESSION_STMT,
    FUNCTION_STMT, IF_STMT, PRINT_STMT, RETURN_STMT, VAR_STMT, WHILE_STMT,

    // Sugar.
    FOR_STMT
}
