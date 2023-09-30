package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;
/*

program        →  statement * EOF;
statement      → exprStmt
               | printStmt ;
exprStmt       → expression ";" ;
printStmt      → print expression ";" ;

expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison ) * ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term ) * ;
term           → factor ( ( "-" | "+" ) factor ) * ;
factor         → unary ( ( "/" | "*" ) unary ) * ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;

*/
public class Parser {

    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        try {
            return program();
        } catch (ParseError error) {
            return null;
        }
    }

    List<Stmt> program() {
        List<Stmt> statements = new ArrayList<>();
        while(!isAtEnd()) {
            statements.add(statement());
        }
        return statements;
    }

    Stmt statement() {
        if(match(PRINT)) return printStmt();
        return expressionStmt();
    }

    private Stmt expressionStmt() {
        var expr = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Expression(expr);
    }

    private Stmt printStmt() {
        var expr = expression();
        consume(SEMICOLON, "Expected ';' after print statement.");
        return new Stmt.Print(expr);
    }


    Expr expression() {
        return equality();
    }



     private Expr equality() {
        return binarayExpr(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return binarayExpr(this::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return binarayExpr(this::factor, MINUS, PLUS);
    }

    private Expr factor() {
        return binarayExpr(this::unary, STAR, SLASH);
    }

    private Expr unary() {
        if(match(MINUS, BANG)) {
            return new Expr.Unary(previous(), unary());
        }
        return primary();
    }

    private Expr primary() {

        if(match(NIL)) return  new Expr.Literal(null);
        if(match(TRUE)) return  new Expr.Literal(true);
        if(match(FALSE)) return  new Expr.Literal(false);

        if(match(NUMBER, STRING)) {
            Object val = previous().literal;
            return new Expr.Literal(val);
        }

        if(match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }


    private Expr binarayExpr(Supplier<Expr> highPrecExpSupplier, TokenType... tokenTypes) {
        Expr expr = highPrecExpSupplier.get();
        while (match(tokenTypes)) {
            Token op = previous();
            Expr right = highPrecExpSupplier.get();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private boolean match(TokenType... tokenTypes) {
        TokenType cTokenType = peek().type;
        for (TokenType type: tokenTypes) {
            if(check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }


    Token peek() {
        return tokens.get(current);
    }

    Token previous() {
        return tokens.get(current-1);
    }
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }



}
