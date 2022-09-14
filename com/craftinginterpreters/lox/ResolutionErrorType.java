package com.craftinginterpreters.lox;

enum ResolutionErrorType {
    VAR_INIT_SELF_REFER,
    BREAK_OUTSIDE_LOOP,
    CONTINUE_OUTSIDE_LOOP,
    RETURN_OUTSIDE_FUNCTION,
    THIS_OUTSIDE_CLASS,
    RETURN_FROM_INIT,
    STATIC_INIT,
}
