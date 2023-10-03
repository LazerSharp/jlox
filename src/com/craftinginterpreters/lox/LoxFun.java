package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFun implements LoxCallable {

    private final Stmt.Fun funStmt;
    LoxFun(Stmt.Fun funStmt) {
        this.funStmt = funStmt;
    }
    @Override
    public int arity() {
        return funStmt.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment funEnv = new Environment(interpreter.environment);
        for (int i=0; i < funStmt.parameters.size(); i++) {
            funEnv.define(funStmt.parameters.get(i).lexeme, arguments.get(i));
        }
        List<Stmt> funBlock = funStmt.block;
        try {
            interpreter.executeBlock(funBlock, funEnv);
            return null;
        } catch (Return ret) {
            return ret.getValue();
        }

    }

    @Override
    public String toString() {
        return "<fun "+ funStmt.name.lexeme + "() ->  LoxFun>";
    }
}
