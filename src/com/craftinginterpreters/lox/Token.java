package com.craftinginterpreters.lox;

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

//    static Token of(TokenType type, int line) {
//        return new Token(type, null, null, line);
//    }


    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
