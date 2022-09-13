package com.craftinginterpreters.lox;

class StrictErrorHandler extends ErrorHandler {
    @Override
    public void handleScanError(int line, String message) {
        System.err.println("[line " + line + "] Syntax error: " + message);
        reportError();
    }

    @Override
    public void handleParseError(ParseRule rule, Token token, String message) {
        String where = getTokenLocation(token);
        System.err.println("[line " + token.line + "] Syntax error at " + where + ": " + message);
        reportError();
    }

    @Override
    public void handleResolutionError(ResolutionErrorType errorType, Token token, String message) {
        reportError();
    }

    @Override
    public void handleRuntimeError(RuntimeError runtimeError) {
        reportRuntimeError();
    }
}
