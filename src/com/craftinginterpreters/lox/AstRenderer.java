package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AstRenderer implements Expr.Visitor<String>, Stmt.Visitor<String> {

    private Map<String, Integer> countMap= new HashMap<>();

    private int count = 0;


    String render(List<Stmt> statements) {

        StringBuilder builder = new StringBuilder();
        builder.append("digraph G {\n");
        for (var stmt: statements) {
            builder.append("Program -> ").append(stmt.accept(this));
        }
        builder.append("\n}");


        return builder.toString();
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        String op = graphElm(expr.operator.type.toString());
        return op + ";\n" +
                op + " -> " + expr.left.accept(this) +
                op + " -> " + expr.right.accept(this) +
                op + " [label=\"" + expr.operator.lexeme + "\"];\n";

    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        String group = graphElm("group");
        return group + ";\n" +
                group + "->" + expr.expression.accept(this) +
                group + " [label=\"(...)\"];\n";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        String lit = graphElm("lit");
        return lit + ";\n" +
                lit + " [label=\""+ expr.value +"\"];\n";
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        String op = graphElm(expr.operator.type.toString());
        return op + ";\n" +
                op + " -> " + expr.right.accept(this) +
                op + " [label=\"" + expr.operator.lexeme + "\"];\n";
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return null;
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return null;
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        return null;
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return null;
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return null;
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        String exprStmt = graphElm("stmt");
        return exprStmt + ";\n" +
                exprStmt + " -> " + stmt.expression.accept(this);
    }

    @Override
    public String visitPrintStmt(Stmt.Print stmt) {
        String printStmt = graphElm("print");
        return printStmt + ";\n" +
                printStmt + " -> " + stmt.expression.accept(this);
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return null;
    }

    @Override
    public String visitFunStmt(Stmt.Fun stmt) {
        return null;
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        return null;
    }

    //String render

    private String graphElm(String prefix) {
        var count = countMap.getOrDefault(prefix, 0);
        var elm = prefix+(count++);
        countMap.put(prefix, count);
        return elm;
    }
}
