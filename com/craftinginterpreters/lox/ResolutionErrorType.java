package com.craftinginterpreters.lox;

enum ResolutionErrorType {
    VAR_INIT_SELF_REFER,
    BREAK_OUTSIDE_LOOP,
    CONTINUE_OUTSIDE_LOOP,
    RETURN_OUTSIDE_FUNCTION,
}
