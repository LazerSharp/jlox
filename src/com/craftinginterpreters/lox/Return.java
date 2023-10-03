package com.craftinginterpreters.lox;

public class Return extends RuntimeException {

    private final Object value;

    Return(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
