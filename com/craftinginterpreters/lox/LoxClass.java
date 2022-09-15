package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods, LoxClass metaclass) {
        super(metaclass);
        this.superclass = superclass;
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            superclass.findMethod(name);
        }

        return null;
    }

    LoxFunction findClassMethod(String name) {
        LoxClass metaclass = getKlass();
        if (metaclass != null && metaclass.methods.containsKey(name)) {
            return metaclass.methods.get(name);
        }

        if (superclass != null) {
            return superclass.findClassMethod(name);
        }

        return null;
    }

    @Override
    public String toString() {
        return "<cls " + name + ">";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}
