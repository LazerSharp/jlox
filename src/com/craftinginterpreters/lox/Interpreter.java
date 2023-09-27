package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object>{



    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr){
        return expr.accept(this);
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

    private String stringify(Object object) {
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

}
