package com.craftinginterpreters.lox;

import java.util.List;

class LoxAnonFunction implements LoxCallable {
    private final Expr.Function definition;
    private final Environment closure;

    LoxAnonFunction(Expr.Function definition, Environment closure) {
        this.definition = definition;
        this.closure = closure;
    }

    @Override
    public String toString() {
        return "<anon fn>";
    }

    @Override
    public int arity() {
        return definition.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < definition.params.size(); i++) {
            environment.define(definition.params.get(i).lexeme, arguments.get(i), true);
        }

        try {
            interpreter.executeBlock(definition.body, environment);
        } catch (Return returnValue) {
            return returnValue;
        }
        return null;
    }
}
