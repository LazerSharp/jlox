package com.craftinginterpreters.lox;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

    Map<Expr, Integer> locals = new HashMap<>();

    Environment globals = new Environment();

    Environment environment = globals;

    Interpreter() {
        globals.define("time", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return new Date().toString();
            }

            @Override
            public String toString() {
                return "<native fun time>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for(var stmt: statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    Object evaluate(Expr expr){
        return expr.accept(this);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        var val = evaluate(expr.expression);
        environment.assign(expr.name, val);
        return val;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        var left = evaluate(expr.left);
        var right = evaluate(expr.right);
        var op = expr.operator;
        return switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperands(op, left, right);
                checkNumberOperands(expr.operator, left, right);
                yield  (double)left - (double)right;
            }
            case PLUS -> {
                if(left instanceof Double && right instanceof Double) {
                    yield  (double)left + (double)right;
                }

                if(left instanceof String && right instanceof String) {
                    yield  (String)left + (String) right;
                }

                throw new RuntimeError(op, "Operands must be numbers / strings");
            }
            case SLASH -> {
                checkNumberOperands(op, left, right);
                yield  (double)left / (double)right;
            }
            case STAR -> {
                checkNumberOperands(op, left, right);
                yield  (double)left * (double)right;
            }
            case GREATER -> {
                checkNumberOperands(op, left, right);
                yield (double)left > (double)right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(op, left, right);
                yield (double)left >= (double)right;
            }
            case LESS -> {
                checkNumberOperands(op, left, right);
                yield (double)left < (double)right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(op, left, right);
                yield (double)left <= (double)right;
            }
            case BANG_EQUAL ->  !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);
            case AND -> {
                if(isTruthy(left)) yield right;
                yield left;
            }
            case OR -> {
                if(!isTruthy(left)) yield right;
                yield left;
            }
            default -> null;
        };
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
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        return switch (expr.operator.type) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield  -(double) right;
            }
            case BANG -> !isTruthy(right);
            default -> null;
        };
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {

        LoxCallable function = (LoxCallable) evaluate(expr.callee);
        List<Expr> arguments = expr.arguments;
        if(expr.arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments.stream().map(this::evaluate).toList());
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        if(!locals.containsKey(expr)) {
            return globals.get(expr.name);
        }
        var distance = locals.get(expr);
        return environment.getAt(distance, expr.name);
    }

    @Override
    public Object visitInlineFunExpr(Expr.InlineFun expr) {
        return new LoxFun(expr, environment);
    }

    private Boolean isTruthy(Object value) {
        if(value == null) return false;
        if(value instanceof Boolean) return (boolean) value;
        return !"".equals(value);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

     String stringify(Object object) {
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

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }



    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        var val = evaluate(stmt.expression);
        System.out.println(stringify(val));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = Constants.UNINITIALIZED;
        if(stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitFunStmt(Stmt.Fun stmt) {
        this.environment.define(stmt.name.lexeme, new LoxFun(stmt, environment));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        throw new Return(evaluate(stmt.expression));
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.ifBranch);
        } else {
            if(stmt.elseBranch != null) {
                execute(stmt.elseBranch);
            }
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.whileStmt);
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

     void executeBlock(List<Stmt> statements, Environment environment) {

        var outer = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt: statements) {
                execute(stmt);
            }
        } finally {
            this.environment = outer;
        }


    }

    public void resolve(Expr.Variable expr, int distance) {
        locals.put(expr, distance);
    }
}
