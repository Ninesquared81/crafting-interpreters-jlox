package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    enum MethodType {
        GETTER, SETTER, NORMAL
    }

    private final Stmt.Function declaration;
    private final Environment closure;

    private final boolean isInitializer;
    private final MethodType methodType;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this(declaration, closure, false, MethodType.NORMAL);
    }

    LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer, MethodType methodType) {
        this.isInitializer = isInitializer;
        this.methodType = methodType;
        this.declaration = declaration;
        this.closure = closure;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer, methodType);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            if (!isInitializer) return returnValue.value;
        }

        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    boolean isGetter() {
        return methodType == MethodType.GETTER;
    }

    boolean isSetter() {
        return methodType == MethodType.SETTER;
    }
}
