package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.craftinginterpreters.lox.LoxFunction.MethodType;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private static class BreakSignal extends RuntimeException {}
    private static class ContinueSignal extends RuntimeException {}

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    private final InputStreamReader input = new InputStreamReader(System.in);
    private final BufferedReader reader = new BufferedReader(input);

    Interpreter() {
        for (var function: Natives.all.entrySet()) {
            globals.define(function.getKey(), function.getValue(), false);
        }
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    void interpret(Expr expression) {
        Object value = "";
        try {
            value = evaluate(expression);
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
        System.out.println(stringify(value));
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        // Signal that we hit a break statement (handled by the loop).
        throw new BreakSignal();
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must  be a class.");
            }
        }

        String className = stmt.name.lexeme;
        environment.declare(className, false);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass, true);
        }

        Map<String, LoxFunction> instanceMethods = new HashMap<>();
        for (Stmt.Function method : stmt.instanceMethods) {
            LoxFunction function = new LoxFunction(method, environment,
                    method.name.lexeme.equals("init"), MethodType.NORMAL);
            instanceMethods.put(method.name.lexeme, function);
        }
        // Treat getters as instance methods
        for (Stmt.Function method : stmt.getters) {
            LoxFunction function = new LoxFunction(method, environment, false, MethodType.GETTER);
            instanceMethods.put(method.name.lexeme, function);
        }
        for (Stmt.Function method : stmt.setters) {
            LoxFunction function = new LoxFunction(method, environment, false, MethodType.SETTER);
            instanceMethods.put(method.name.lexeme, function);
        }

        Map<String, LoxFunction> classMethods = new HashMap<>();
        for (Stmt.Function method : stmt.classMethods) {
            LoxFunction function = new LoxFunction(method, environment, false, MethodType.NORMAL);
            classMethods.put(method.name.lexeme, function);
        }

        LoxClass metaclass = null;
        if (!classMethods.isEmpty()) {
            metaclass = new LoxClass(null, null, classMethods, null);
        }

        LoxClass class_ = new LoxClass(className, (LoxClass)superclass, instanceMethods, metaclass);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.define(stmt.name.lexeme, class_, false);

        if (metaclass != null) metaclass.set(stmt.name, class_);

        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueSignal();
    }

    @Override
    public Void visitEmptyStmt(Stmt.Empty stmt) {
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function, false);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitInputStmt(Stmt.Input stmt) {
        String input;
        try {
            input = reader.readLine();
        } catch (IOException e) {
            throw new RuntimeError(stmt.keyword, "There was an error reading input.");
        }
        Object value = parseInput(input);
        environment.assign(stmt.variable.name, value);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitValStmt(Stmt.Val stmt) {
        Object value = evaluate(stmt.initializer);
        environment.define(stmt.name.lexeme, value, false);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value, true);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakSignal signal) {
                // If a break statement was reached, break
                break;
            } catch (ContinueSignal signal) {
                continue;
            }
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        Object condition = evaluate(expr.condition);

        // Short-circuit.
        if (isTruthy(condition)) return evaluate(expr.left);

        // If condition false.
        return evaluate(expr.right);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                if (left instanceof String && right instanceof String) {
                    return ((String)left).compareTo((String)right) > 0;
                }
                if (left instanceof Double && right instanceof Double) return (double)left > (double)right;

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case GREATER_EQUAL:
                if (left instanceof String && right instanceof String) {
                    return ((String)left).compareTo((String)right) >= 0;
                }
                if (left instanceof Double && right instanceof Double) return (double)left >= (double)right;

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case LESS:
                if (left instanceof String && right instanceof String) {
                    return ((String)left).compareTo((String)right) < 0;
                }
                if (left instanceof Double && right instanceof Double) return (double)left < (double)right;

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case LESS_EQUAL:
                if (left instanceof String && right instanceof String) {
                    return ((String)left).compareTo((String)right) <= 0;
                }
                if (left instanceof Double && right instanceof Double) return (double)left <= (double)right;

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }

                throw new RuntimeError(
                        expr.operator,
                        "Operands must be two numbers or a string and another object."
                );
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0.0) throw new RuntimeError(expr.operator, "Division by zero.");
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case COMMA: return right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments but got "
                    + arguments.size() + "."
            );
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        return new LoxAnonFunction(expr, environment);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            Object value = ((LoxInstance)object).get(expr.name);
            if ((value instanceof LoxFunction) && ((LoxFunction) value).isGetter()) {
                return ((LoxFunction) value).call(this, Collections.emptyList());
            }

            return value;
        }

        throw new RuntimeError(expr.name, "Only instances have properties");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);

        // A setter has a "=" suffix.
        LoxFunction setter = ((LoxInstance) object).getMethod(expr.name.lexeme + "=");
        if (setter != null) {
            setter.call(this, Collections.singletonList(value));
        } else {
            ((LoxInstance) object).set(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        LoxClass superclass = (LoxClass) environment.getAt(distance, "super");

        LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");

        LoxFunction method = superclass.findMethod(expr.method.lexeme);
        if (method == null) method = superclass.findClassMethod(expr.method.lexeme);

        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        }

        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private Object parseInput(String input) {
        if (input.equals("nil")) return null;
        if (input.equals("true")) return Boolean.TRUE;
        if (input.equals("false")) return Boolean.FALSE;
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ignored) {}

        // If other checks fail, it's just a string.
        return input;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        LoxCallable function = Natives.all.get("stringify");
        return (String) function.call(this, Collections.singletonList(object));
    }
}
