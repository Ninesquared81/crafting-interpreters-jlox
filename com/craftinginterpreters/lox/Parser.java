package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;
import static com.craftinginterpreters.lox.ParseRule.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private final ErrorHandler errorHandler;
    private int current = 0;

    Parser(List<Token> tokens, ErrorHandler errorHandler) {
        this.tokens = tokens;
        this.errorHandler = errorHandler;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Expr expression() {
        return comma();
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUN)) {
                if (check(LEFT_PAREN)) {
                    Expr expr = functionExpression();
                    consume(FUNCTION_EXPR, SEMICOLON, "Expect ';' after anonymous function expression.");
                    return new Stmt.Expression(expr);
                }
                return function("function");
            }
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> instanceMethods = new ArrayList<>();
        List<Stmt.Function> classMethods = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            if (match(CLASS)) {
                classMethods.add(function("method"));
            } else {
                instanceMethods.add(function("method"));
            }
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, instanceMethods, classMethods);
    }

    private Stmt statement() {
        if (match((CONTINUE))) return continueStatement();
        if (match(BREAK)) return breakStatement();
        if (match(SEMICOLON)) return emptyStatement();
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt continueStatement() {
        Token keyword = previous();
        consume(BREAK_STMT, SEMICOLON, "Expect ';' after 'continue'.");

        return new Stmt.Continue(keyword);
    }

    private Stmt breakStatement() {
        Token keyword = previous();
        consume(BREAK_STMT, SEMICOLON, "Expect ';' after 'break'.");

        return new Stmt.Break(keyword);
    }

    private Stmt emptyStatement() {
        return new Stmt.Empty();
    }

    private Stmt forStatement() {
        consume(FOR_STMT, LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(FOR_STMT, SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(FOR_STMT, RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(
                            body,
                            new Stmt.Expression(increment)
                    )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(IF_STMT, LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(IF_STMT, RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(PRINT_STMT, SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(RETURN_STMT, SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(VAR_STMT, IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(VAR_STMT, SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(WHILE_STMT, LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(WHILE_STMT, RIGHT_PAREN, "Expect ')' after condition.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(EXPRESSION_STMT, SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(FUNCTION_STMT, IDENTIFIER, "Expect " + kind + " name.");
        consume(FUNCTION_STMT, LEFT_PAREN, "Expect '(' after " + kind + " name.");

        return functionContent(kind, name);
    }

    private Stmt.Function functionContent(String kind, Token name) {
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(FUNCTION_STMT, peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(FUNCTION_STMT, IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(FUNCTION_STMT, RIGHT_PAREN, "Expect ')' after parameters.");
        consume(FUNCTION_STMT, LEFT_BRACE, "Expect '{' before " + kind + " body.");

        List<Stmt> body = block();

        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(BLOCK_STMT, RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr comma() {
        Expr expr = assignment();
        while (match(COMMA)) {
            Token operator = previous();
            Expr right = assignment();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr assignment() {
        Expr expr = conditional();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(ASSIGN_EXPR, equals, "Invalid assignment target.");
        }

        return  expr;
    }

    private Expr conditional() {
        Expr expr = or();

        if (match(QUESTION_MARK)) {
            // Parse left (middle) as if it is parenthesized.
            Expr left = expression();
            if (!match(COLON)) {
                throw error(CONDITIONAL_EXPR, peek(), "Expect ':'");
            }
            Expr right = conditional();
            expr = new Expr.Conditional(expr, left, right);
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(CALL_EXPR, peek(), "Can't have more than 255 arguments.");
                }
                // Use equality to avoid getting commas in call confused with comma operator.
                arguments.add(equality());
            } while (match(COMMA));
        }

        Token paren = consume(CALL_EXPR, RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(FUN)) {
            return functionExpression();
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(GROUPING_EXPR, RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(PRIMARY_EXPR, peek(), "Expect expression.");
    }

    private Expr.Function functionExpression() {
        Token keyword = previous();
        consume(FUNCTION_EXPR, LEFT_PAREN, "Expect '(' after 'fun' in expression.");
        Stmt.Function function = functionContent("function", null);

        return new Expr.Function(keyword, function.params, function.body);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        return consume(UNKNOWN, type, message);
    }

    private Token consume(ParseRule rule, TokenType type, String message) {
        if (check(type)) return advance();

        throw error(rule, peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(ParseRule rule, Token token, String message) {
        errorHandler.handleParseError(rule, token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS: case FOR: case FUN: case IF: case PRINT: case RETURN: case VAR: case WHILE:
                    return;
            }

            advance();
        }
    }
}
