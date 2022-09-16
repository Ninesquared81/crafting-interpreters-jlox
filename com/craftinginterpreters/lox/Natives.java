package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Natives {
    abstract static class NativeFunction implements LoxCallable {
        @Override
        public String toString() {
            return "<native fn>";
        }
    }
    static final Map<String, LoxCallable> all;
    static {
        all = new HashMap<>();
        all.put("clock", new NativeFunction() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

        });
        all.put("rand", new NativeFunction() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return Math.random();
            }
        });
    }
}