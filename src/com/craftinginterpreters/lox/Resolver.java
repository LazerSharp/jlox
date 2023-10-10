package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Stmt.Visitor<Void>, Expr.Visitor<Void> {

    private enum FunType {
        NONE,
        FUNCTION
    }
    private FunType currentFun = FunType.NONE;

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {

        beginScope();
        resolve(stmt.statements);
        endScope();

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        resolve(stmt.initializer);
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name,
                    "Can't read local variable in its own initializer.");
        }

        var varName = expr.name.lexeme;
        var size = scopes.size();
        for(var i = size - 1; i >= 0; i--) {
            if(scopes.get(i).containsKey(varName) && scopes.get(i).get(varName)) {
                interpreter.resolve(expr, size - 1 -i);
                break;
            };
        }
        return null;
    }

    private void define(Token name) {
        if(scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void declare(Token name) {
        if(scopes.isEmpty()) return;
        var scope = scopes.peek();
        if(scope.containsKey(name.lexeme)) {
            Lox.error(name,"Variable '" + name.lexeme +"' is already declared in this scope.");
        }
        scope.put(name.lexeme, false);
    }


    private void beginScope() {
        scopes.add(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    void resolve(List<Stmt> statements) {
        for(Stmt stmt: statements) {
            resolve(stmt);
        }
    }

    private void resolveExpressions(List<Expr> expressions) {
        for(Expr expr: expressions) {
            resolve(expr);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }
    private void resolve(Expr expr) {
        expr.accept(this);
    }


    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        // no variable: skip
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        resolveExpressions(expr.arguments);
        return null;
    }

    @Override
    public Void visitInlineFunExpr(Expr.InlineFun expr) {
        FunType enclosingFun = currentFun;
        currentFun = FunType.FUNCTION;
        beginScope();
        for(Token param : expr.parameters) {
            declare(param);
            define(param);
        }
        resolve(expr.statements);
        endScope();
        currentFun = enclosingFun;
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.ifBranch);
        if(stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.whileStmt);
        return null;
    }



    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }



    @Override
    public Void visitFunStmt(Stmt.Fun stmt) {

        declare(stmt.name);
        define(stmt.name);

        FunType enclosingFun = currentFun;
        currentFun = FunType.FUNCTION;

        beginScope();
        for(Token param: stmt.parameters) {
            declare(param);
            define(param);
        }
        resolve(stmt.block);
        endScope();

        currentFun = enclosingFun;
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if(currentFun == FunType.NONE) {
            Lox.error(stmt.keyword, "return statement not inside a function.");
        }
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        return null;
    }
}
