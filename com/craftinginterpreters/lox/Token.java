package com.craftinginterpreters.lox;

public class Token {
    final com.craftinginterpreters.lox.TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }

    Token rename(String name) {
        return new Token(this.type, name, this.literal, this.line);
    }
}
