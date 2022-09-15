package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private record ValueInfo(Object value, boolean isMutable) {}
    final Environment enclosing;
    private final Map<String, ValueInfo> values = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme).value;
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            if (!values.get(name.lexeme).isMutable) {
                throw new RuntimeError(name, "Cannot reassign constant.");
            }
            values.put(name.lexeme, new ValueInfo(value, true));
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void declare(String name, boolean isMutable) {
        values.put(name, new ValueInfo(null, isMutable));
    }

    void define(String name, Object value, boolean isMutable) {
        values.put(name, new ValueInfo(value, isMutable));
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            assert environment != null;
            environment = environment.enclosing;
        }

        return environment;
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name).value;
    }

    void assignAt(int distance, Token name, Object value) {
        Environment target = ancestor(distance);
        if (!target.values.get(name.lexeme).isMutable) {
            throw new RuntimeError(name, "Cannot reassign constant value.");
        }
        target.values.put(name.lexeme, new ValueInfo(value, true));
    }
}
