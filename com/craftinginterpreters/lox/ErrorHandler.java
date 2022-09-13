package com.craftinginterpreters.lox;

abstract class ErrorHandler {
    abstract void handleScanError(int line, String message);
    abstract void handleParseError(ParseRule rule, Token token, String message);
    abstract void handleResolutionError(ResolutionErrorType errorType, Token token, String message);
    abstract void handleRuntimeError(RuntimeError runtimeError);

    protected void reportError() {
        Lox.hadError = true;
    }

    protected void reportRuntimeError() {
        Lox.hadRuntimeError = true;
    }

    protected String getTokenLocation(Token token) {
        return (token.type == TokenType.EOF) ? "end" : "'" + token.lexeme + "'";
    }
}
