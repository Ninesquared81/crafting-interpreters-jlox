package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign : Token name, Expr value",
                "Binary : Expr left, Token operator, Expr right",
                "Call : Expr callee, Token paren, List<Expr> arguments",
                "Conditional : Expr condition, Expr left, Expr right",
                "Function : Token keyword, List<Token> params, List<Stmt> body",
                "Get : Expr object, Token name",
                "Grouping : Expr expression",
                "Literal : Object value",
                "Logical : Expr left, Token operator, Expr right",
                "Set : Expr object, Token name, Expr value",
                "Super : Token keyword, Token method",
                "This : Token keyword",
                "Unary : Token operator, Expr right",
                "Variable : Token name"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block : List<Stmt> statements",
                "Break : Token keyword",
                "Class : Token name, Expr.Variable superclass, " +
                        "List<Stmt.Function> instanceMethods, List<Stmt.Function> classMethods, " +
                        "List<Stmt.Function> getters, List<Stmt.Function> setters",
                "Continue : Token keyword",
                "Empty",
                "Expression : Expr expression",
                "Function : Token name, List<Token> params, List<Stmt> body",
                "If : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Input : Token keyword, Expr.Variable variable",
                "Print : Expr expression",
                "Return : Token keyword, Expr value",
                "Val : Token name, Expr initializer",
                "Var : Token name, Expr initializer",
                "While : Expr condition, Stmt body"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writer.println("package com.craftinginterpreters.lox;");
            writer.println();
            writer.println("import java.util.List;");
            writer.println();
            writer.println("abstract class " + baseName + " {");

            defineVisitor(writer, baseName, types);

            // The AST classes.
            for (String type : types) {
                List<String> splitResult = new ArrayList<>(Arrays.asList(type.split(":")));
                splitResult.add("");  // Ensure the length of the list is at least 2.

                String className = splitResult.get(0).trim();
                String fields = splitResult.get(1).trim();

                defineType(writer, baseName, className, fields);
            }

            // The base accept() method.
            writer.println();
            writer.println("    abstract <R> R accept(Visitor<R> visitor);");

            writer.println("}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "("
                    + typeName + " " + baseName.toLowerCase() + ");");
        }

        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("    static class " + className + " extends " + baseName + " {");

        if (!fieldList.isEmpty()) {
            printConstructor(writer, className, fieldList);
            writer.println();
        }

        printAcceptMethod(writer, baseName, className);

        if (!fieldList.isEmpty()) {
            writer.println();
            printFields(writer, fieldList);
        }

        writer.println("    }");
    }

    private static void printConstructor(PrintWriter writer, String className, String fieldList) {
        // Constructor.
        writer.println("        " + className + "(" + fieldList + ") {");
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }

        writer.println("        }");
    }

    private static void printAcceptMethod(PrintWriter writer, String baseName, String className) {
        // Visitor pattern.
        writer.println("        @Override");
        writer.println("        <R> R accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit" + className + baseName + "(this);");
        writer.println("        }");
    }

    private static void printFields(PrintWriter writer, String fieldList) {
        // Fields.
        String[] fields = fieldList.split(", ");
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }
    }
}
