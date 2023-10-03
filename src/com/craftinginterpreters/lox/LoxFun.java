package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFun implements LoxCallable {

    private final List<Token> parameters;
    private final List<Stmt> statements;
    private final Environment closure;

    private final String name;
    LoxFun(Stmt.Fun funStmt, Environment closure) {
        this.parameters = funStmt.parameters;
        this.statements = funStmt.block;
        this.name = funStmt.name.lexeme;
        this.closure = closure;
    }

    LoxFun(Expr.InlineFun funStmt, Environment closure) {
        this.parameters = funStmt.parameters;
        this.statements = funStmt.statements;
        this.closure = closure;
        this.name = "(_inline_)";
    }
    @Override
    public int arity() {
        return parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment funEnv = new Environment(closure);
        for (int i=0; i < parameters.size(); i++) {
            funEnv.define(parameters.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(statements, funEnv);
            return null;
        } catch (Return ret) {
            return ret.getValue();
        }

    }

    @Override
    public String toString() {
        return "<fun "+ name + "() ->  LoxFun>";
    }
}
