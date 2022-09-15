package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        LoxFunction classMethod = klass.findClassMethod(name.lexeme);
        if (classMethod != null) return classMethod.bind(klass);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return "<inst " + klass.name + ">";
    }

    LoxClass getKlass() {
        return klass;
    }

    LoxFunction getMethod(String name) {
        LoxFunction method = klass.findMethod(name);
        if (method == null) method = klass.findClassMethod(name);

        if (method == null) return null;

        return method.bind(this);
    }
}
