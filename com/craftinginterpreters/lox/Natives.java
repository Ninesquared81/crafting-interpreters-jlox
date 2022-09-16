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
        all.put("round", new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object number = arguments.get(0);
                return Math.floor((Double) number + 0.5);
            }
        });
        all.put("isNumber", new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return arguments.get(0) instanceof Double;
            }
        });
        all.put(("isString"), new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return arguments.get(0) instanceof String;
            }
        });
        all.put("abs", new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object number = arguments.get(0);
                return Math.abs((Double) number);
            }
        });
        all.put("lowercase", new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object string = arguments.get(0);
                return ((String) string).toLowerCase();
            }
        });
        all.put("uppercase", new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object string = arguments.get(0);
                return ((String) string).toUpperCase();
            }
        });
        all.put("stringify", new NativeFunction() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object object = arguments.get(0);
                if (object == null) return "nil";

                if (object instanceof Double) {
                    String text = object.toString();
                    if (text.endsWith(".0")) {
                        text = text.substring(0, text.length() - 2);
                    }
                    return text;
                }

                return object.toString();
            }
        });
    }
}