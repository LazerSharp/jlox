package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    private final Environment enclosing;

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        if(values.containsKey(name.lexeme)) {
            Object val = values.get(name.lexeme);
            if(val == Constants.UNINITIALIZED){
                throw new RuntimeError(name,
                        "Uninitialized variable '" + name.lexeme + "'.");
            }
            return val;
        }
        if(this.enclosing != null) {
            return enclosing.get(name);
        }
        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if(this.enclosing != null) {
            this.enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    public Object getAt(Integer distance, Token name) {
        return ancestor(distance).values.get(name.lexeme);
    }

    private Environment ancestor(Integer distance) {
        Environment environment = this;
        for(var i=0; i < distance;i++) {
            environment = environment.enclosing;
        }
        return environment;
    }
}
